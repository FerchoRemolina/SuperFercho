package com.superfercho.assistant.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.assistant.application.fercho.FerchoAlertDecision;
import com.superfercho.assistant.application.fercho.FerchoAlertRecord;
import com.superfercho.assistant.application.fercho.FerchoRestrictionRecord;
import com.superfercho.assistant.application.fercho.FerchoSupervisionPort;
import com.superfercho.assistant.application.fercho.FerchoUsageTotals;
import com.superfercho.assistant.application.fercho.FerchoUsageLedgerUnavailableException;
import com.superfercho.assistant.application.exception.FerchoPausedException;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import com.superfercho.assistant.infrastructure.configuration.FerchoCostProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FerchoAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AssistantExceptionHandler.class)
@EnableConfigurationProperties(FerchoCostProperties.class)
class FerchoAdminControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-18T15:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ALERT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID RESTRICTION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FerchoSupervisionPort supervision;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ADMIN_ID);
    }

    @Test
    void summaryIsServedForAdminWithTotalsAndCounts() throws Exception {
        when(supervision.totals(any(Instant.class), any(Instant.class))).thenReturn(
                new FerchoUsageTotals(                NOW.minus(java.time.Duration.ofHours(24)), NOW, 12, 3, 18, 5400L, 900L, null));
        when(supervision.countActiveAutoPauses(any(Instant.class))).thenReturn(1);
        when(supervision.countActiveSuspensions(any(Instant.class))).thenReturn(0);
        when(supervision.countOpenAlerts()).thenReturn(2);

        mockMvc.perform(get("/api/v1/admin/fercho/summary").param("windowHours", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedRequests").value(12))
                .andExpect(jsonPath("$.rejectedRequests").value(3))
                .andExpect(jsonPath("$.llmCalls").value(18))
                .andExpect(jsonPath("$.estimatedCost").doesNotExist())
                .andExpect(jsonPath("$.activeAutoPauses").value(1))
                .andExpect(jsonPath("$.openAlerts").value(2));
    }

    @Test
    void rejectsInvalidWindowHours() throws Exception {
        mockMvc.perform(get("/api/v1/admin/fercho/summary").param("windowHours", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsAlertsMappedWithoutEntities() throws Exception {
        when(supervision.findAlerts("OPEN", 50)).thenReturn(List.of(new FerchoAlertRecord(
                ALERT_ID, USER_ID, "OPEN", "RECIDIVE_LIMIT_EXCEEDED",
                NOW.minus(java.time.Duration.ofHours(1)), 3, 4, 5400, 900, null, "gpt-4o-mini",
                NOW.minus(java.time.Duration.ofMinutes(30)), null, null, null, null)));

        mockMvc.perform(get("/api/v1/admin/fercho/alerts").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ALERT_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].promptTokens").value(5400));
    }

    @Test
    void resolvesAlertWithTheAdminIdFromTheAuthenticatedContext() throws Exception {
        when(supervision.resolveAlert(
                eq(ALERT_ID), any(), eq(ADMIN_ID), eq("revisado"), eq(Duration.ofHours(24)), any(Instant.class)))
                .thenReturn(RESTRICTION_ID);

        mockMvc.perform(post("/api/v1/admin/fercho/alerts/" + ALERT_ID + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"SUSPEND\",\"note\":\"revisado\",\"suspensionHours\":24}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertId").value(ALERT_ID.toString()))
                .andExpect(jsonPath("$.suspensionId").value(RESTRICTION_ID.toString()));
    }

    @Test
    void rejectsInvalidDecisionActions() throws Exception {
        mockMvc.perform(post("/api/v1/admin/fercho/alerts/" + ALERT_ID + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"DELETE_EVERYTHING\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void liftsARestrictionWithAudit() throws Exception {
        when(supervision.findRestriction(RESTRICTION_ID)).thenReturn(Optional.of(new FerchoRestrictionRecord(
                RESTRICTION_ID, USER_ID, "ADMIN_SUSPENSION", "ADMIN_DECISION",
                NOW, ADMIN_ID, NOW.plus(Duration.ofHours(24)), NOW.plus(Duration.ofMinutes(5)), ADMIN_ID, "abuso")));

        mockMvc.perform(post("/api/v1/admin/fercho/restrictions/" + RESTRICTION_ID + "/lift")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"revisión manual\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liftedAt").isNotEmpty())
                .andExpect(jsonPath("$.liftedBy").value(ADMIN_ID.toString()));
        verify(supervision).liftSuspension(eq(RESTRICTION_ID), eq(ADMIN_ID), eq("revisión manual"), any(Instant.class));
    }

    @Test
    void returnsControlled503WhenTheLedgerIsUnavailable() throws Exception {
        when(supervision.totals(any(Instant.class), any(Instant.class)))
                .thenThrow(new FerchoUsageLedgerUnavailableException("ledger down", null));

        mockMvc.perform(get("/api/v1/admin/fercho/summary"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("FERCHO_UNAVAILABLE"));
    }

    @Test
    void pauseExceptionMapsTo429WithServerTime() throws Exception {
        var pausedUntil = NOW.plus(Duration.ofMinutes(30));
        when(supervision.totals(any(Instant.class), any(Instant.class)))
                .thenThrow(new FerchoPausedException("FERCHO_PAUSED", "pausa", 1800, pausedUntil));

        mockMvc.perform(get("/api/v1/admin/fercho/summary"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("FERCHO_PAUSED"))
                .andExpect(jsonPath("$.remainingSeconds").value(1800))
                .andExpect(jsonPath("$.pausedUntil").value(pausedUntil.toString()));
    }
}
