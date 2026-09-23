package com.superfercho.catalog.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.catalog.application.dto.BarcodeProductSuggestion;
import com.superfercho.catalog.application.usecase.LookupProductByBarcodeUseCase;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.infrastructure.security.IdentitySecurityConfiguration;
import com.superfercho.identity.infrastructure.security.JwtAccessTokenService;
import com.superfercho.platform.time.ClockConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductBarcodeLookupController.class)
@Import({IdentitySecurityConfiguration.class, ClockConfiguration.class, CatalogExceptionHandler.class})
@TestPropertySource(
        properties = {
            "superfercho.security.jwt.secret=test-only-superfercho-jwt-secret-key-32b",
            "superfercho.security.jwt.expiration=15m"
        })
class ProductBarcodeLookupSecurityTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @MockitoBean
    private LookupProductByBarcodeUseCase lookupProductByBarcodeUseCase;

    @Test
    void adminCanLookup() throws Exception {
        when(lookupProductByBarcodeUseCase.execute(any()))
                .thenReturn(new BarcodeProductSuggestion("3017620422003", "Nutella", null, null, null));

        mockMvc.perform(get("/api/v1/products/barcode-lookup/{barcode}", "3017620422003")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nutella"));
    }

    @Test
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/products/barcode-lookup/{barcode}", "3017620422003")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/products/barcode-lookup/{barcode}", "3017620422003"))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(Role role) {
        return "Bearer " + jwtAccessTokenService.issue(USER_ID, role).token();
    }
}
