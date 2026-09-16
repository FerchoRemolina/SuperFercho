package com.superfercho.identity.infrastructure.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.application.usecase.ActivateCategoryUseCase;
import com.superfercho.catalog.application.usecase.CreateCategoryUseCase;
import com.superfercho.catalog.application.usecase.DeactivateCategoryUseCase;
import com.superfercho.catalog.application.usecase.GetCategoryUseCase;
import com.superfercho.catalog.application.usecase.ListCategoriesUseCase;
import com.superfercho.catalog.application.usecase.UpdateCategoryUseCase;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.infrastructure.rest.CategoryController;
import com.superfercho.identity.application.dto.AuthenticationResult;
import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.usecase.AddAddressUseCase;
import com.superfercho.identity.application.usecase.AuthenticateUserUseCase;
import com.superfercho.identity.application.usecase.DeactivateAddressUseCase;
import com.superfercho.identity.application.usecase.ListAddressesUseCase;
import com.superfercho.identity.application.usecase.RegisterCustomerUseCase;
import com.superfercho.identity.application.usecase.SetDefaultAddressUseCase;
import com.superfercho.identity.application.usecase.UpdateAddressUseCase;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.identity.infrastructure.rest.AddressController;
import com.superfercho.identity.infrastructure.rest.AuthController;
import com.superfercho.identity.infrastructure.rest.CustomerController;
import com.superfercho.orders.application.dto.OrderItemResult;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.ShippingAddressResult;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.application.usecase.ListOrdersUseCase;
import com.superfercho.orders.application.usecase.UpdateOrderStatusUseCase;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.infrastructure.configuration.TransactionalCancelOrderUseCase;
import com.superfercho.orders.infrastructure.configuration.TransactionalCheckoutUseCase;
import com.superfercho.orders.infrastructure.rest.OrderController;
import com.superfercho.platform.money.Money;
import com.superfercho.platform.time.ClockConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(
        controllers = {
            AuthController.class,
            CustomerController.class,
            AddressController.class,
            CategoryController.class,
            OrderController.class
        })
@Import({IdentitySecurityConfiguration.class, ClockConfiguration.class})
@TestPropertySource(
        properties = {
            "superfercho.security.jwt.secret=test-only-superfercho-jwt-secret-key-32b",
            "superfercho.security.jwt.expiration=15m"
        })
class HttpAuthorizationSecurityTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    @MockitoBean
    private RegisterCustomerUseCase registerCustomerUseCase;

    @MockitoBean
    private AddAddressUseCase addAddressUseCase;

    @MockitoBean
    private ListAddressesUseCase listAddressesUseCase;

    @MockitoBean
    private UpdateAddressUseCase updateAddressUseCase;

    @MockitoBean
    private DeactivateAddressUseCase deactivateAddressUseCase;

    @MockitoBean
    private SetDefaultAddressUseCase setDefaultAddressUseCase;

    @MockitoBean
    private CreateCategoryUseCase createCategoryUseCase;

    @MockitoBean
    private GetCategoryUseCase getCategoryUseCase;

    @MockitoBean
    private ListCategoriesUseCase listCategoriesUseCase;

    @MockitoBean
    private UpdateCategoryUseCase updateCategoryUseCase;

    @MockitoBean
    private ActivateCategoryUseCase activateCategoryUseCase;

    @MockitoBean
    private DeactivateCategoryUseCase deactivateCategoryUseCase;

    @MockitoBean
    private TransactionalCheckoutUseCase transactionalCheckoutUseCase;

    @MockitoBean
    private TransactionalCancelOrderUseCase transactionalCancelOrderUseCase;

    @MockitoBean
    private GetOrderUseCase getOrderUseCase;

    @MockitoBean
    private ListOrdersUseCase listOrdersUseCase;

    @MockitoBean
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @BeforeEach
    void stubUseCases() {
        when(authenticateUserUseCase.execute(any()))
                .thenReturn(new AuthenticationResult(USER_ID, Role.CUSTOMER, "token", NOW));
        when(registerCustomerUseCase.execute(any())).thenReturn(registeredCustomer());
        when(listAddressesUseCase.execute()).thenReturn(List.of());
        when(listCategoriesUseCase.execute(any())).thenReturn(List.of());
        when(createCategoryUseCase.execute(any())).thenReturn(categoryResult());
        when(updateOrderStatusUseCase.execute(any())).thenReturn(orderResult());
    }

    @Test
    void shouldAllowLoginWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ada@identity.test", "password": "secret-password"}
                                """))
                .andExpect(notBlockedBySecurity());
    }

    @Test
    void shouldAllowCustomerRegistrationWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "CC",
                                  "documentNumber": "1234567890",
                                  "fullName": "Ada Lovelace",
                                  "email": "ada@identity.test",
                                  "phone": "3001234567",
                                  "password": "secret-password"
                                }
                                """))
                .andExpect(notBlockedBySecurity());
    }

    @Test
    void shouldAllowPublicCatalogGetWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/categories")).andExpect(notBlockedBySecurity());
        mockMvc.perform(get("/api/v1/products")).andExpect(notBlockedBySecurity());
    }

    @Test
    void shouldAllowPublicCatalogViewWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/categories").param("view", "PUBLIC")).andExpect(notBlockedBySecurity());
        mockMvc.perform(get("/api/v1/products").param("view", "PUBLIC")).andExpect(notBlockedBySecurity());
    }

    @Test
    void shouldRejectCustomerRouteWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/addresses")).andExpect(unauthenticated());
    }

    @Test
    void shouldAllowCustomerRouteWithCustomerJwt() throws Exception {
        mockMvc.perform(get("/api/v1/addresses").header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER)))
                .andExpect(notBlockedBySecurity());
    }

    @Test
    void shouldRejectCustomerRouteWithAdminJwt() throws Exception {
        mockMvc.perform(get("/api/v1/addresses").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(accessDenied());
    }

    @Test
    void shouldRejectAdminRouteWithCustomerJwt() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Lácteos", "description": "Leche y derivados"}
                                """))
                .andExpect(accessDenied());
    }

    @Test
    void shouldRejectAdminRouteWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Lácteos", "description": "Leche y derivados"}
                                """))
                .andExpect(unauthenticated());
    }

    @Test
    void shouldAllowAdminRouteWithAdminJwt() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Lácteos", "description": "Leche y derivados"}
                                """))
                .andExpect(notBlockedBySecurity());
    }

    @Test
    void shouldRejectOrderStatusUpdateWithCustomerJwt() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{orderId}/status", ORDER_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PREPARING"}
                                """))
                .andExpect(accessDenied());
    }

    @Test
    void shouldAllowOrderStatusUpdateWithAdminJwt() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{orderId}/status", ORDER_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PREPARING"}
                                """))
                .andExpect(notBlockedBySecurity());
    }

    @Test
    void shouldRejectAdminCatalogViewWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/categories").param("view", "ADMIN")).andExpect(unauthenticated());
        mockMvc.perform(get("/api/v1/products").param("view", "ADMIN")).andExpect(unauthenticated());
        mockMvc.perform(get("/api/v1/products/search").param("view", "ADMIN")).andExpect(unauthenticated());
    }

    @Test
    void shouldRejectAdminCatalogViewWithCustomerJwt() throws Exception {
        mockMvc.perform(get("/api/v1/categories")
                        .param("view", "ADMIN")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER)))
                .andExpect(accessDenied());
    }

    @Test
    void shouldAllowAdminCatalogViewWithAdminJwt() throws Exception {
        mockMvc.perform(get("/api/v1/categories")
                        .param("view", "ADMIN")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(notBlockedBySecurity());
    }

    @Test
    void shouldRejectInvalidBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/addresses").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(unauthenticated());
    }

    private String bearer(Role role) {
        return "Bearer " + jwtAccessTokenService.issue(USER_ID, role).token();
    }

    private static ResultMatcher notBlockedBySecurity() {
        return result -> {
            int status = result.getResponse().getStatus();
            if (status == 401 || status == 403) {
                throw new AssertionError("Security blocked the request with HTTP " + status);
            }
        };
    }

    private static ResultMatcher unauthenticated() {
        return result -> {
            status().isUnauthorized().match(result);
            content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).match(result);
            jsonPath("$.code").value("UNAUTHENTICATED").match(result);
        };
    }

    private static ResultMatcher accessDenied() {
        return result -> {
            status().isForbidden().match(result);
            content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).match(result);
            jsonPath("$.code").value("ACCESS_DENIED").match(result);
        };
    }

    private static RegisteredCustomer registeredCustomer() {
        return new RegisteredCustomer(
                USER_ID,
                "CC",
                "1234567890",
                "Ada Lovelace",
                "ada@identity.test",
                "3001234567",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                NOW);
    }

    private static CategoryResult categoryResult() {
        return new CategoryResult(
                CATEGORY_ID, "Lácteos", "Leche y derivados", CategoryStatus.ACTIVE, NOW, NOW);
    }

    private static OrderResult orderResult() {
        Money total = Money.cop(new BigDecimal("21.00"));
        return new OrderResult(
                ORDER_ID,
                "ORD-P-1001",
                OrderStatus.PREPARING,
                List.of(new OrderItemResult(
                        UUID.fromString("99999999-9999-9999-9999-000000000001"),
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        "Leche entera",
                        total,
                        2,
                        total)),
                total,
                total,
                new ShippingAddressResult(
                        "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567"),
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                NOW,
                null,
                null,
                NOW);
    }
}
