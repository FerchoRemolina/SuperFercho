package com.superfercho.assistant.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.superfercho.assistant.application.dto.chat.ChatCommand;
import com.superfercho.assistant.application.dto.chat.ChatResponse;
import com.superfercho.assistant.application.dto.llm.LlmMessage;
import com.superfercho.assistant.application.dto.llm.LlmResponse;
import com.superfercho.assistant.application.port.out.ClockPort;
import com.superfercho.assistant.application.port.out.ConversationStore;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.port.out.LLMPort;
import com.superfercho.assistant.application.port.out.PendingSensitiveActionStore;
import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolRegistry;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.infrastructure.confirmation.InMemoryPendingSensitiveActionStore;
import com.superfercho.assistant.infrastructure.conversation.InMemoryConversationStore;
import com.superfercho.assistant.infrastructure.llm.FakeLlmAdapter;
import com.superfercho.assistant.application.tool.orders.CancelOrderTool;
import com.superfercho.assistant.application.tool.shopping.GetCartTool;
import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.ShippingAddressResult;
import com.superfercho.orders.application.port.in.CancelOrderUseCase;
import com.superfercho.orders.application.port.in.CheckoutUseCase;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import com.superfercho.shopping.domain.model.CartStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ChatApplicationServiceToolLoggingTest {

    private static final Instant NOW = Instant.parse("2026-09-18T15:00:00Z");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORDER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ClockPort clockPort;

    @Mock
    private GetCartUseCase getCartUseCase;

    @Mock
    private CheckoutUseCase checkoutUseCase;

    @Mock
    private CancelOrderUseCase cancelOrderUseCase;

    @Mock
    private GetOrderUseCase getOrderUseCase;

    private FakeLlmAdapter llm;
    private InMemoryConversationStore conversations;
    private InMemoryPendingSensitiveActionStore pending;
    private final com.superfercho.assistant.application.fercho.FerchoUsageGate ferchoUsageGate =
            org.mockito.Mockito.mock(com.superfercho.assistant.application.fercho.FerchoUsageGate.class);
    private final com.superfercho.assistant.application.fercho.FerchoLimits ferchoLimits =
            new com.superfercho.assistant.application.fercho.FerchoLimits(
                    30, java.time.Duration.ofHours(24), java.time.Duration.ofMinutes(30), java.time.Duration.ofHours(24));
    private ChatApplicationService chat;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger chatLogger;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient()
                .when(ferchoUsageGate.evaluateAndReserve(
                                org.mockito.ArgumentMatchers.any(),
                                org.mockito.ArgumentMatchers.any(),
                                org.mockito.ArgumentMatchers.any()))
                .thenReturn(com.superfercho.assistant.application.fercho.FerchoTurnDecision.accepted(
                        UUID.randomUUID()));
        llm = new FakeLlmAdapter();
        conversations = new InMemoryConversationStore();
        pending = new InMemoryPendingSensitiveActionStore();
        List<AssistantTool> tools = List.of(
                new GetCartTool(getCartUseCase),
                new CancelOrderTool(currentUserProvider, getOrderUseCase, clockPort, pending));
        chat = new ChatApplicationService(
                currentUserProvider,
                clockPort,
                conversations,
                pending,
                llm,
                new ToolRegistry(tools),
                checkoutUseCase,
                cancelOrderUseCase,
                ferchoUsageGate,
                ferchoLimits,
                new com.superfercho.assistant.infrastructure.packagesel.InMemoryPackageSelectionProposalStore(),
                org.mockito.Mockito.mock(com.superfercho.shopping.application.port.in.ApplyIdempotentCartAddUseCase.class),
                org.mockito.Mockito.mock(com.superfercho.catalog.application.usecase.GetProductUseCase.class));

        chatLogger = (Logger) LoggerFactory.getLogger(ChatApplicationService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        chatLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        chatLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    private List<String> ferchoLines() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.startsWith("FERCHO_"))
                .toList();
    }

    /** El contrato del evento es su prefijo; la duración al final es variable. */
    private boolean hasEvent(String prefix) {
        return ferchoLines().stream().anyMatch(line -> line.startsWith(prefix));
    }

    private ChatResponse runCustomerTurn(String message) {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        return chat.execute(new ChatCommand(null, message, null));
    }

    @Test
    void shouldLogSuccessfulToolExecution() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getCartUseCase.execute()).thenReturn(emptyCart());
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall("c1", "get_cart", Map.of()))));
        llm.enqueue(LlmResponse.text("Listo."));

        chat.execute(new ChatCommand(null, "ver carrito", null));

        List<String> lines = ferchoLines();
        assertTrue(hasEvent("FERCHO_TOOL round=1 tool=get_cart status=SUCCESS"),
                "eventos capturados: " + listAppender.list.size() + " -> " + lines);
        assertTrue(hasEvent("FERCHO_TRACE rounds=2 tools=get_cart"),
                "eventos capturados: " + listAppender.list.size() + " -> " + lines);
        assertFalse(lines.stream().anyMatch(line -> line.contains(USER_ID.toString())));
    }

    @Test
    void shouldLogFailedToolExecution() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getCartUseCase.execute()).thenThrow(new IllegalStateException("boom"));
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall("c1", "get_cart", Map.of()))));
        llm.enqueue(LlmResponse.text("Hubo un problema."));

        chat.execute(new ChatCommand(null, "ver carrito", null));

        List<String> lines = ferchoLines();
        assertTrue(hasEvent("FERCHO_TOOL round=1 tool=get_cart status=FAILURE"));
        assertTrue(hasEvent("FERCHO_TRACE rounds=2 tools=get_cart"));
        assertFalse(lines.stream().anyMatch(line -> line.contains("boom")));
    }

    @Test
    void shouldLogConfirmationRequiredToolExecution() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getOrderUseCase.execute(new GetOrderCommand(ORDER_ID))).thenReturn(cancellableOrder());
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall(
                "c1", "cancel_order", Map.of("orderId", ORDER_ID.toString())))));
        llm.enqueue(LlmResponse.text("¿Confirmas la cancelación?"));

        ChatResponse response = chat.execute(new ChatCommand(null, "cancela mi pedido", null));

        assertTrue(response.awaitingConfirmation());
        List<String> lines = ferchoLines();
        assertTrue(hasEvent("FERCHO_TOOL round=1 tool=cancel_order status=CONFIRMATION_REQUIRED"));
        assertTrue(hasEvent("FERCHO_TRACE rounds=1 tools=cancel_order"));
        assertFalse(lines.stream().anyMatch(line -> line.contains(ORDER_ID.toString())));
    }

    @Test
    void shouldLogMultipleRoundsAndToolTrace() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getCartUseCase.execute()).thenReturn(emptyCart());
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall("c1", "get_cart", Map.of()))));
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall("c2", "get_cart", Map.of()))));
        llm.enqueue(LlmResponse.text("Listo."));

        chat.execute(new ChatCommand(null, "ver carrito dos veces", null));

        List<String> lines = ferchoLines();
        assertTrue(hasEvent("FERCHO_TOOL round=1 tool=get_cart status=SUCCESS"));
        assertTrue(hasEvent("FERCHO_TOOL round=2 tool=get_cart status=SUCCESS"));
        assertTrue(hasEvent("FERCHO_TRACE rounds=3 tools=get_cart,get_cart"));
    }

    private static CartResponse emptyCart() {
        return CartResponse.of(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                USER_ID,
                CartStatus.ACTIVE,
                List.of(),
                NOW,
                NOW);
    }

    private static OrderResult cancellableOrder() {
        return new OrderResult(
                ORDER_ID,
                "SF-1",
                USER_ID,
                OrderStatus.CONFIRMED,
                List.of(),
                Money.cop(new java.math.BigDecimal("10.50")),
                Money.cop(new java.math.BigDecimal("10.50")),
                new ShippingAddressResult("Ada", "Calle 1", null, "Bogotá", "Cundinamarca", "300"),
                UUID.randomUUID(),
                NOW,
                NOW,
                null,
                NOW,
                null);
    }
}
