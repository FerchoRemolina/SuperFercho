package com.superfercho.assistant.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.assistant.application.exception.FerchoPausedException;
import com.superfercho.assistant.application.fercho.FerchoUsageLedgerUnavailableException;
import com.superfercho.assistant.application.fercho.FerchoAlertDecision;
import com.superfercho.assistant.application.fercho.FerchoSupervisionPort;
import com.superfercho.assistant.application.fercho.FerchoUsageTotals;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.infrastructure.configuration.FerchoCostProperties;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.infrastructure.security.IdentitySecurityConfiguration;
import com.superfercho.identity.infrastructure.security.JwtAccessTokenService;
import com.superfercho.platform.time.ClockConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FerchoAdminController.class)
@Import({IdentitySecurityConfiguration.class, ClockConfiguration.class, AssistantExceptionHandler.class})
@EnableConfigurationProperties(FerchoCostProperties.class)
@TestPropertySource(properties = {
        "superfercho.security.jwt.secret=test-only-superfercho-jwt-secret-key-32b",
        "superfercho.security.jwt.expiration=20m"
})
class FerchoAdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @MockitoBean
    private FerchoSupervisionPort supervision;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private String bearer(UUID userId, Role role) {
        var issued = jwtAccessTokenService.issue(userId, role);
        return "Bearer " + issued.token();
    }

    private static final UUID ADMIN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void adminCanAccessSummary() throws Exception {
        when(supervision.totals(any(Instant.class), any(Instant.class))).thenReturn(emptyTotals());
        when(currentUserProvider.getCurrentUserId()).thenReturn(ADMIN_ID);
        mockMvc.perform(get("/api/v1/admin/fercho/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_ID, Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void customerIsForbiddenOnSummary() throws Exception {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        mockMvc.perform(get("/api/v1/admin/fercho/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID, Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsUnauthorizedOnSummary() throws Exception {
        mockMvc.perform(get("/api/v1/admin/fercho/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerIsForbiddenOnDecision() throws Exception {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        mockMvc.perform(post("/api/v1/admin/fercho/alerts/" + ALERT_ID + "/decision")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID, Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"DISMISS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerIsForbiddenOnLift() throws Exception {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        mockMvc.perform(post("/api/v1/admin/fercho/restrictions/" + RESTRICTION_ID + "/lift")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID, Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ledgerFailureReturns503WithNoModelCall() throws Exception {
        when(supervision.totals(any(Instant.class), any(Instant.class)))
                .thenThrow(new FerchoUsageLedgerUnavailableException("ledger down", null));
        mockMvc.perform(get("/api/v1/admin/fercho/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_ID, Role.ADMIN)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("FERCHO_UNAVAILABLE"));
    }

    private static FerchoUsageTotals emptyTotals() {
        return new FerchoUsageTotals(
                Instant.now().minus(Duration.ofHours(24)), Instant.now(), 0, 0, 0, null, null, null);
    }

    private static final UUID ALERT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID RESTRICTION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
}
