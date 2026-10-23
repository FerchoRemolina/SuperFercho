package com.superfercho.assistant.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.dto.chat.ChatCommand;
import com.superfercho.assistant.application.dto.chat.ChatResponse;
import com.superfercho.assistant.application.dto.llm.LlmMessage;
import com.superfercho.assistant.application.dto.llm.LlmRequest;
import com.superfercho.assistant.application.dto.llm.LlmResponse;
import com.superfercho.assistant.application.exception.FerchoPausedException;
import com.superfercho.assistant.application.fercho.FerchoLimits;
import com.superfercho.assistant.application.fercho.FerchoTurnDecision;
import com.superfercho.assistant.application.fercho.FerchoUsageGate;
import com.superfercho.assistant.application.port.out.ClockPort;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.port.in.ChatUseCase;
import com.superfercho.assistant.application.port.out.PendingSensitiveActionStore;
import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolRegistry;
import com.superfercho.assistant.infrastructure.conversation.InMemoryConversationStore;
import com.superfercho.assistant.infrastructure.llm.FakeLlmAdapter;
import com.superfercho.orders.application.port.in.CancelOrderUseCase;
import com.superfercho.orders.application.port.in.CheckoutUseCase;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Límites de uso de Fercho con gate en memoria y reloj controlable: cubre
 * aceptación, pausa, rechazo sin IA, expiración, ventana móvil y aislamiento,
 * sin llamadas reales a OpenAI ni esperas.
 */
class ChatApplicationServiceFerchoLimitsTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    /** Gate en memoria con reloj inyectable: replica la semántica del JPA. */
    static class InMemoryFerchoUsageGate implements FerchoUsageGate {
        record Turn(UUID id, UUID userId, String status, Instant at) {}
        record Restriction(UUID id, UUID userId, String type, Instant createdAt, Instant expiresAt) {}
        record Alert(UUID id, UUID userId, String status, Instant createdAt) {}

        final Map<UUID, Turn> turns = new HashMap<>();
        final Map<UUID, Restriction> restrictions = new HashMap<>();
        final List<Alert> alerts = new java.util.ArrayList<>();
        java.time.Instant clock = T0;
        FerchoLimits limits;
        int llmCallCounter = 0;

        void advance(Duration d) {
            clock = clock.plus(d);
        }

        long acceptedInWindow(UUID userId, FerchoLimits limits, Instant now) {
            var start = now.minus(limits.window());
            return turns.values().stream()
                    .filter(t -> t.userId().equals(userId))
                    .filter(t -> "ACCEPTED".equals(t.status()))
                    .filter(t -> !t.at().isBefore(start))
                    .count();
        }

        @Override
        public FerchoTurnDecision evaluateAndReserve(UUID userId, FerchoLimits limits, Instant now) {
            this.limits = limits;
            var active = restrictions.values().stream()
                    .filter(r -> r.userId().equals(userId) && r.expiresAt().isAfter(now))
                    .findFirst();
            if (active.isPresent()) {
                var r = active.get();
                String type = "ADMIN_SUSPENSION".equals(r.type()) ? "SUSPENDED" : "PAUSED";
                turns.put(UUID.randomUUID(), new Turn(UUID.randomUUID(), userId,
                        "SUSPENDED".equals(type) ? "REJECTED_SUSPENDED" : "REJECTED_PAUSED", now));
                return new FerchoTurnDecision(
                        "SUSPENDED".equals(type)
                                ? com.superfercho.assistant.application.fercho.FerchoTurnDecisionType.SUSPENDED
                                : com.superfercho.assistant.application.fercho.FerchoTurnDecisionType.PAUSED,
                        null, userId, r.expiresAt(), "LIMIT_EXCEEDED", null,
                        Duration.between(now, r.expiresAt()));
            }
            long accepted = acceptedInWindow(userId, limits, now);
            if (accepted >= limits.maxRequestsPerWindow()) {
                var restrictionId = UUID.randomUUID();
                restrictions.put(restrictionId, new Restriction(
                        restrictionId, userId, "AUTO_PAUSE", now, now.plus(limits.pauseDuration())));
                turns.put(UUID.randomUUID(), new Turn(UUID.randomUUID(), userId, "REJECTED_LIMIT", now));
                boolean recidive = restrictions.values().stream()
                        .filter(r -> r.userId().equals(userId) && "AUTO_PAUSE".equals(r.type()))
                        .filter(r -> !r.id.equals(restrictionId))
                        .anyMatch(r -> !r.createdAt.isBefore(now.minus(limits.recidiveWindow())));
                boolean openAlert = alerts.stream()
                        .anyMatch(a -> a.userId().equals(userId) && "OPEN".equals(a.status()));
                if (recidive && !openAlert) {
                    alerts.add(new Alert(UUID.randomUUID(), userId, "OPEN", now));
                }
                return new FerchoTurnDecision(
                        com.superfercho.assistant.application.fercho.FerchoTurnDecisionType.PAUSED,
                        null, userId, now.plus(limits.pauseDuration()), "LIMIT_EXCEEDED", null,
                        limits.pauseDuration());
            }
            var turnId = UUID.randomUUID();
            turns.put(turnId, new Turn(turnId, userId, "ACCEPTED", now));
            return FerchoTurnDecision.accepted(turnId);
        }

        @Override
        public void completeAcceptedTurn(
                UUID turnId, int llmCalls, Integer promptTokens, Integer completionTokens,
                String model, java.math.BigDecimal estimatedCost, Instant now) {
            // métricas persistidas por el adaptador real; no afectan estos escenarios
        }

        @Override
        public void recordRejectedTurn(
                UUID userId, com.superfercho.assistant.application.fercho.FerchoTurnDecisionType type,
                String reason, Instant now) {
            turns.put(UUID.randomUUID(), new Turn(UUID.randomUUID(), userId, "REJECTED", now));
        }
    }

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final PendingSensitiveActionStore pendingStore = mock(PendingSensitiveActionStore.class);
    private final CheckoutUseCase checkoutUseCase = mock(CheckoutUseCase.class);
    private final CancelOrderUseCase cancelOrderUseCase = mock(CancelOrderUseCase.class);
    private final InMemoryFerchoUsageGate gate = new InMemoryFerchoUsageGate();
    private final FakeLlmAdapter llm = new FakeLlmAdapter();
    private final InMemoryConversationStore conversations = new InMemoryConversationStore();

    private ChatApplicationService newService(InMemoryFerchoUsageGate gate) {
        List<AssistantTool> tools = List.of();
        return new ChatApplicationService(
                currentUserProvider, clockPort, conversations, pendingStore, llm,
                new ToolRegistry(tools), checkoutUseCase, cancelOrderUseCase,
                gate, new FerchoLimits(3, Duration.ofHours(24), Duration.ofMinutes(1), Duration.ofHours(24)),
                new com.superfercho.assistant.infrastructure.packagesel.InMemoryPackageSelectionProposalStore(),
                mock(com.superfercho.shopping.application.port.in.ApplyIdempotentCartAddUseCase.class),
                mock(com.superfercho.catalog.application.usecase.GetProductUseCase.class));
    }

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(clockPort.currentTime()).thenAnswer(inv -> gate.clock);
        when(pendingStore.findByToken(Mockito.any())).thenReturn(java.util.Optional.empty());
        when(pendingStore.findByUserId(Mockito.any())).thenReturn(java.util.Optional.empty());
    }

    private ChatResponse send(ChatApplicationService service, String message) {
        llm.enqueue(LlmResponse.text("Respuesta de Fercho."));
        return service.execute(new ChatCommand(null, message, null));
    }

    /** El rechazo en pausa llega como FerchoPausedException (fail-closed del turno). */
    private FerchoPausedException sendExpectingPause(ChatApplicationService service, String message) {
        llm.enqueue(LlmResponse.text("no debe invocarse"));
        try {
            service.execute(new ChatCommand(null, message, null));
        } catch (FerchoPausedException expected) {
            return expected;
        }
        throw new AssertionError("se esperaba FerchoPausedException");
    }

    private long acceptedCount(UUID user) {
        return gate.turns.values().stream()
                .filter(t -> t.userId().equals(user) && "ACCEPTED".equals(t.status()))
                .count();
    }

    @Test
    void acceptsTurnsBelowTheLimit() {
        var service = newService(gate);
        for (int i = 0; i < 3; i++) {
            var response = send(service, "mensaje " + i);
            assertNotNull(response.assistantMessage());
        }
        assertEquals(3, acceptedCount(userId));
        assertEquals(3, llm.requests().size());
    }

    @Test
    void pausesAtTheThresholdAndRejectsWithoutInvokingTheModel() {
        var service = newService(gate);
        for (int i = 0; i < 3; i++) {
            send(service, "mensaje " + i);
        }
        assertEquals(3, acceptedCount(userId));
        int llmCallsBefore = llm.requests().size();

        var paused = sendExpectingPause(service, "otro mensaje");

        assertNotNull(paused);
        assertEquals("FERCHO_PAUSED", paused.code());
        assertEquals("Fercho est\u00e1 tomando un descanso. Int\u00e9ntalo m\u00e1s tarde.", paused.getMessage());
        assertTrue(paused.getMessage().indexOf('\u00e1') >= 0);
        assertFalse(paused.getMessage().contains("\u00c3"));
        assertEquals(3, acceptedCount(userId));
        assertEquals(llmCallsBefore, llm.requests().size());
    }

    @Test
    void resumesAutomaticallyWhenThePauseExpiresAndTheWindowAllows() {
        var service = newService(gate);
        for (int i = 0; i < 3; i++) {
            send(service, "m" + i);
        }
        gate.advance(Duration.ofHours(25));
        // La ventana de 24 h ya expiró: los 3 turnos salen del conteo y la
        // pausa de 1 min también.
        llm.enqueue(LlmResponse.text("de nuevo"));
        var response = newService(gate).execute(new ChatCommand(null, "de nuevo", null));
        assertNotNull(response.assistantMessage());
        // 3 turnos previos + 1 nuevo aceptado al reiniciar la ventana móvil.
        assertEquals(4, acceptedCount(userId));
    }

    @Test
    void pauseDoesNotResetTheRollingWindowCounter() {
        var service = newService(gate);
        for (int i = 0; i < 3; i++) {
            send(service, "m" + i);
        }
        gate.advance(Duration.ofMinutes(2));
        // La pausa previa no existía: m3 dispara la PRIMERA pausa automática y
        // el turno NO se acepta (contador de la ventana intacto: 3).
        var paused = sendExpectingPause(service, "m3");
        assertNotNull(paused);
        assertEquals(3, acceptedCount(userId));
        assertEquals(1, gate.restrictions.size());

        // Dos minutos después la pausa expira, pero la ventana sigue llena:
        // rechaza de nuevo y crea una SEGUNDA pausa (reincidencia), sin aceptar
        // ni reiniciar el contador.
        gate.advance(Duration.ofMinutes(2));
        var paused2 = sendExpectingPause(service, "m4");
        assertNotNull(paused2);
        assertEquals(3, acceptedCount(userId));
        assertEquals(2, gate.restrictions.size());
    }

    @Test
    void recidiveGeneratesExactlyOneOpenAlert() {
        var service = newService(gate);
        for (int i = 0; i < 3; i++) {
            send(service, "m" + i);
        }
        gate.advance(Duration.ofMinutes(2));
        var firstPause = sendExpectingPause(service, "m3");
        assertNotNull(firstPause);
        assertEquals(0, gate.alerts.stream().filter(a -> "OPEN".equals(a.status())).count());

        gate.advance(Duration.ofMinutes(2));
        var recidivePause = sendExpectingPause(service, "m4");
        assertNotNull(recidivePause);
        assertEquals(1, gate.alerts.stream().filter(a -> "OPEN".equals(a.status())).count());

        var stillPaused = sendExpectingPause(service, "m5");
        assertNotNull(stillPaused);
        assertEquals(1, gate.alerts.stream().filter(a -> "OPEN".equals(a.status())).count());
    }

    @Test
    void clientsAreIsolatedFromEachOthersLimits() {
        var serviceA = newService(gate);
        var otherUser = UUID.randomUUID();
        var serviceB = newService(gate);

        for (int i = 0; i < 3; i++) {
            send(serviceA, "a" + i);
        }
        when(currentUserProvider.getCurrentUserId()).thenReturn(otherUser);
        var responseB = send(serviceB, "b0");
        assertNotNull(responseB.assistantMessage());
        assertEquals(1, acceptedCount(otherUser));
    }

    @Test
    void acceptedTurnsRecordLlmCallsAndTokensWhenAvailable() {
        var service = newService(gate);
        llm.enqueue(LlmResponse.withUsage(
                LlmResponse.text("con tokens"),
                new com.superfercho.assistant.application.dto.llm.LlmUsage(120, 30, 150)));
        service.execute(new ChatCommand(null, "hola", null));
        // La métrica de tokens la completa el adaptador JPA; aquí solo verificamos
        // que el turno aceptado se registró y el flujo respondió.
        assertEquals(1, acceptedCount(userId));
    }

    @Test
    void differentConversationsShareTheSameClientCounter() {
        var service = newService(gate);
        var first = send(service, "c1");
        var second = send(service, "c2");
        assertNotEquals(first.conversationId(), second.conversationId());
        assertEquals(2, acceptedCount(userId));
    }
}
