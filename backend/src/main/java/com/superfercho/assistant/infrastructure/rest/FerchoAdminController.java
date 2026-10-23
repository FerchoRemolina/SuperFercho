package com.superfercho.assistant.infrastructure.rest;

import com.superfercho.assistant.application.exception.FerchoPausedException;
import com.superfercho.assistant.application.fercho.FerchoUsageLedgerUnavailableException;
import com.superfercho.assistant.application.fercho.FerchoAlertDecision;
import com.superfercho.assistant.application.fercho.FerchoAlertRecord;
import com.superfercho.assistant.application.fercho.FerchoClientUsageSummary;
import com.superfercho.assistant.application.fercho.FerchoRestrictionRecord;
import com.superfercho.assistant.application.fercho.FerchoSupervisionPort;
import com.superfercho.assistant.application.fercho.FerchoUsageTotals;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Supervisión administrativa de Fercho (solo ADMIN). El administrador se
 * identifica desde el contexto autenticado; nunca desde el request.
 */
@RestController
@Profile("!test")
@RequestMapping("/api/v1/admin/fercho")
public class FerchoAdminController {

    private static final int MAX_LIMIT = 50;
    private static final int MAX_WINDOW_HOURS = 720;
    private static final Set<String> DECISIONS = Set.of("DISMISS", "WARN", "SUSPEND");

    private final FerchoSupervisionPort supervision;
    private final com.superfercho.assistant.infrastructure.configuration.FerchoCostProperties costProperties;
    private final CurrentUserProvider currentUserProvider;

    public FerchoAdminController(
            FerchoSupervisionPort supervision,
            com.superfercho.assistant.infrastructure.configuration.FerchoCostProperties costProperties,
            CurrentUserProvider currentUserProvider) {
        this.supervision = supervision;
        this.costProperties = costProperties;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/summary")
    public FerchoAdminSummaryRestResponse summary(@RequestParam(defaultValue = "24") int windowHours) {
        requireWindow(windowHours);
        var now = Instant.now();
        var start = now.minus(Duration.ofHours(windowHours));
        FerchoUsageTotals totals = supervision.totals(start, now);
        return new FerchoAdminSummaryRestResponse(
                start, now, totals.acceptedRequests(), totals.rejectedRequests(), totals.llmCalls(),
                totals.promptTokens(), totals.completionTokens(), totals.estimatedCost(),
                costProperties.calculable(), supervision.countActiveAutoPauses(now),
                supervision.countActiveSuspensions(now), supervision.countOpenAlerts());
    }

    @GetMapping("/alerts")
    public List<FerchoAlertRestResponse> alerts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        requireLimit(limit);
        return supervision.findAlerts(status, limit).stream().map(FerchoAlertRestResponse::from).toList();
    }

    @GetMapping("/alerts/{alertId}")
    public FerchoAlertRestResponse alert(@PathVariable UUID alertId) {
        return supervision.findAlert(alertId).map(FerchoAlertRestResponse::from).orElse(null);
    }

    @PostMapping("/alerts/{alertId}/decision")
    public FerchoAdminDecisionRestResponse decide(
            @PathVariable UUID alertId, @RequestBody FerchoAlertDecisionRequest request) {
        requireDecision(request.action());
        var adminId = currentUserProvider.getCurrentUserId();
        var now = Instant.now();
        var suspensionDuration = request.suspensionHours() == null
                ? null
                : Duration.ofHours(request.suspensionHours());
        UUID restrictionId = supervision.resolveAlert(
                alertId, FerchoAlertDecision.valueOf(request.action()), adminId, request.note(),
                suspensionDuration, now);
        return new FerchoAdminDecisionRestResponse(alertId, request.action(), adminId, now, restrictionId);
    }

    @GetMapping("/restrictions")
    public List<FerchoRestrictionRestResponse> restrictions(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "50") int limit) {
        requireLimit(limit);
        var records = activeOnly
                ? supervision.findActiveRestrictions(limit)
                : supervision.findRestrictionsForUser(currentUserProvider.getCurrentUserId(), limit);
        return records.stream().map(FerchoRestrictionRestResponse::from).toList();
    }

    @PostMapping("/restrictions/{restrictionId}/lift")
    public FerchoRestrictionRestResponse lift(
            @PathVariable UUID restrictionId, @RequestBody(required = false) FerchoLiftRequest request) {
        var adminId = currentUserProvider.getCurrentUserId();
        supervision.liftSuspension(
                restrictionId, adminId, request == null ? null : request.note(), Instant.now());
        return supervision.findRestriction(restrictionId).map(FerchoRestrictionRestResponse::from).orElse(null);
    }

    @GetMapping("/clients/{userId}/usage")
    public FerchoClientUsageRestResponse clientUsage(
            @PathVariable UUID userId, @RequestParam(defaultValue = "24") int windowHours) {
        requireWindow(windowHours);
        var now = Instant.now();
        FerchoClientUsageSummary summary =
                supervision.clientUsage(userId, now.minus(Duration.ofHours(windowHours)), now);
        return new FerchoClientUsageRestResponse(
                summary.userId(), summary.windowStart(), summary.windowEnd(), summary.acceptedRequests(),
                summary.rejectedRequests(), summary.llmCalls(), summary.promptTokens(), summary.completionTokens(),
                summary.estimatedCost(), summary.model(), summary.activeRestrictionId(),
                summary.activeRestrictionType(), summary.activeRestrictionExpiresAt());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        var problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        problem.setDetail(exception.getMessage());
        problem.setProperty("code", "INVALID_ADMIN_REQUEST");
        return problem;
    }

    private static void requireWindow(int windowHours) {
        if (windowHours < 1 || windowHours > MAX_WINDOW_HOURS) {
            throw new IllegalArgumentException("windowHours must be between 1 and " + MAX_WINDOW_HOURS);
        }
    }

    private static void requireLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
    }

    private static void requireDecision(String action) {
        if (action == null || !DECISIONS.contains(action)) {
            throw new IllegalArgumentException("action must be one of " + DECISIONS);
        }
    }

    public record FerchoAdminSummaryRestResponse(
            Instant windowStart, Instant windowEnd, long acceptedRequests, long rejectedRequests,
            long llmCalls, Long promptTokens, Long completionTokens, java.math.BigDecimal estimatedCost,
            boolean costEstimated, int activeAutoPauses, int activeSuspensions, int openAlerts) {
    }

    public record FerchoAlertRestResponse(
            UUID id, UUID userId, String status, String reason, Instant windowStartedAt,
            int acceptedRequests, int llmCalls, Integer promptTokens, Integer completionTokens,
            java.math.BigDecimal estimatedCost, String model, Instant createdAt, Instant decidedAt,
            UUID decidedBy, String decisionNote, UUID restrictionId) {

        public static FerchoAlertRestResponse from(FerchoAlertRecord record) {
            return new FerchoAlertRestResponse(
                    record.id(), record.userId(), record.status(), record.reason(), record.windowStartedAt(),
                    record.acceptedRequests(), record.llmCalls(), record.promptTokens(),
                    record.completionTokens(), record.estimatedCost(), record.model(), record.createdAt(),
                    record.decidedAt(), record.decidedBy(), record.decisionNote(), record.restrictionId());
        }
    }

    public record FerchoRestrictionRestResponse(
            UUID id, UUID userId, String type, String reason, Instant createdAt, UUID createdBy,
            Instant expiresAt, Instant liftedAt, UUID liftedBy, String note) {

        public static FerchoRestrictionRestResponse from(FerchoRestrictionRecord record) {
            return new FerchoRestrictionRestResponse(
                    record.id(), record.userId(), record.type(), record.reason(), record.createdAt(),
                    record.createdBy(), record.expiresAt(), record.liftedAt(), record.liftedBy(), record.note());
        }
    }

    public record FerchoClientUsageRestResponse(
            UUID userId, Instant windowStart, Instant windowEnd, int acceptedRequests, int rejectedRequests,
            int llmCalls, Integer promptTokens, Integer completionTokens, java.math.BigDecimal estimatedCost,
            String model, UUID activeRestrictionId, String activeRestrictionType,
            Instant activeRestrictionExpiresAt) {
    }

    public record FerchoAlertDecisionRequest(String action, String note, Integer suspensionHours) {
    }

    public record FerchoLiftRequest(String note) {
    }

    public record FerchoAdminDecisionRestResponse(
            UUID alertId, String action, UUID adminId, Instant decidedAt, UUID suspensionId) {
    }
}
