package com.superfercho.catalog.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.catalog.application.dto.ProductTypeResult;
import com.superfercho.catalog.application.usecase.ActivateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.CreateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.GetProductTypeUseCase;
import com.superfercho.catalog.application.usecase.ListProductTypesUseCase;
import com.superfercho.catalog.application.usecase.UpdateProductTypeUseCase;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.infrastructure.security.IdentitySecurityConfiguration;
import com.superfercho.identity.infrastructure.security.JwtAccessTokenService;
import com.superfercho.platform.time.ClockConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductTypeController.class)
@Import({IdentitySecurityConfiguration.class, ClockConfiguration.class, CatalogExceptionHandler.class})
@TestPropertySource(
        properties = {
            "superfercho.security.jwt.secret=test-only-superfercho-jwt-secret-key-32b",
            "superfercho.security.jwt.expiration=15m"
        })
class ProductTypeControllerSecurityTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CATEGORY_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TYPE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @MockitoBean
    private CreateProductTypeUseCase createProductTypeUseCase;

    @MockitoBean
    private GetProductTypeUseCase getProductTypeUseCase;

    @MockitoBean
    private ListProductTypesUseCase listProductTypesUseCase;

    @MockitoBean
    private UpdateProductTypeUseCase updateProductTypeUseCase;

    @MockitoBean
    private ActivateProductTypeUseCase activateProductTypeUseCase;

    @MockitoBean
    private DeactivateProductTypeUseCase deactivateProductTypeUseCase;

    @Test
    void adminCanListProductTypes() throws Exception {
        when(listProductTypesUseCase.execute(any()))
                .thenReturn(List.of(new ProductTypeResult(
                        TYPE_ID, CATEGORY_ID, "Leche", null, ProductTypeStatus.ACTIVE, NOW, NOW)));

        mockMvc.perform(get("/api/v1/product-types")
                        .param("categoryId", CATEGORY_ID.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Leche"));
    }

    @Test
    void adminCanCreateProductType() throws Exception {
        when(createProductTypeUseCase.execute(any()))
                .thenReturn(new ProductTypeResult(
                        TYPE_ID, CATEGORY_ID, "Leche", null, ProductTypeStatus.ACTIVE, NOW, NOW));

        mockMvc.perform(post("/api/v1/product-types")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": "%s", "name": "Leche", "description": null}
                                """.formatted(CATEGORY_ID)))
                .andExpect(status().isCreated());
    }

    @Test
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/product-types")
                        .param("categoryId", CATEGORY_ID.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/product-types").param("categoryId", CATEGORY_ID.toString()))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(Role role) {
        return "Bearer " + jwtAccessTokenService.issue(USER_ID, role).token();
    }
}
