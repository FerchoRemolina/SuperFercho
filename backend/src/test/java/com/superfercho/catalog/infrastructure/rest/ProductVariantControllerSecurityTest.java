package com.superfercho.catalog.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.catalog.application.dto.ProductVariantResult;
import com.superfercho.catalog.application.usecase.ActivateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.CreateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.GetProductVariantUseCase;
import com.superfercho.catalog.application.usecase.ListProductVariantsUseCase;
import com.superfercho.catalog.application.usecase.UpdateProductVariantUseCase;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
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

@WebMvcTest(controllers = ProductVariantController.class)
@Import({IdentitySecurityConfiguration.class, ClockConfiguration.class, CatalogExceptionHandler.class})
@TestPropertySource(
        properties = {
            "superfercho.security.jwt.secret=test-only-superfercho-jwt-secret-key-32b",
            "superfercho.security.jwt.expiration=20m"
        })
class ProductVariantControllerSecurityTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TYPE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VARIANT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @MockitoBean
    private CreateProductVariantUseCase createProductVariantUseCase;

    @MockitoBean
    private GetProductVariantUseCase getProductVariantUseCase;

    @MockitoBean
    private ListProductVariantsUseCase listProductVariantsUseCase;

    @MockitoBean
    private UpdateProductVariantUseCase updateProductVariantUseCase;

    @MockitoBean
    private ActivateProductVariantUseCase activateProductVariantUseCase;

    @MockitoBean
    private DeactivateProductVariantUseCase deactivateProductVariantUseCase;

    @Test
    void adminCanListProductVariants() throws Exception {
        when(listProductVariantsUseCase.execute(any()))
                .thenReturn(List.of(new ProductVariantResult(
                        VARIANT_ID, TYPE_ID, "Entera", null, ProductVariantStatus.ACTIVE, NOW, NOW)));

        mockMvc.perform(get("/api/v1/product-variants")
                        .param("productTypeId", TYPE_ID.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Entera"));
    }

    @Test
    void adminCanCreateProductVariant() throws Exception {
        when(createProductVariantUseCase.execute(any()))
                .thenReturn(new ProductVariantResult(
                        VARIANT_ID, TYPE_ID, "Entera", null, ProductVariantStatus.ACTIVE, NOW, NOW));

        mockMvc.perform(post("/api/v1/product-variants")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productTypeId": "%s", "name": "Entera", "description": null}
                                """.formatted(TYPE_ID)))
                .andExpect(status().isCreated());
    }

    @Test
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/product-variants")
                        .param("productTypeId", TYPE_ID.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/product-variants").param("productTypeId", TYPE_ID.toString()))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(Role role) {
        return "Bearer " + jwtAccessTokenService.issue(USER_ID, role).token();
    }
}
