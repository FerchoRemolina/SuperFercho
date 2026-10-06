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
import com.superfercho.orders.application.port.in.CancelOrderUseCase;
import com.superfercho.orders.application.port.in.CheckoutUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
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

    private FakeLlmAdapter llm;
    private InMemoryConversationStore conversations;
    private InMemoryPendingSensitiveActionStore pending;
    private ChatApplicationService chat;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger chatLogger;

    @BeforeEach
    void setUp() {
        llm = new FakeLlmAdapter();
        conversations = new InMemoryConversationStore();
        pending = new InMemoryPendingSensitiveActionStore();
        List<AssistantTool> tools = List.of(
                new GetCartTool(getCartUseCase),
                new CancelOrderTool(currentUserProvider, pending));
        chat = new ChatApplicationService(
                currentUserProvider,
                clockPort,
                conversations,
                pending,
                llm,
                new ToolRegistry(tools),
                checkoutUseCase,
                cancelOrderUseCase);

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

    private ChatResponse runCustomerTurn(String message) {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        return chat.execute(new ChatCommand(null, message, null));
    }

    @Test
    void shouldLogSuccessfulToolExecution() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getCartUseCase.execute()).thenReturn(null);
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall("c1", "get_cart", Map.of()))));
        llm.enqueue(LlmResponse.text("Listo."));

        chat.execute(new ChatCommand(null, "ver carrito", null));

        List<String> lines = ferchoLines();
        assertTrue(lines.contains("FERCHO_TOOL round=1 tool=get_cart status=SUCCESS"),
                "eventos capturados: " + listAppender.list.size() + " -> " + lines);
        assertTrue(lines.contains("FERCHO_TRACE rounds=2 tools=get_cart"),
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
        assertTrue(lines.contains("FERCHO_TOOL round=1 tool=get_cart status=FAILURE"));
        assertTrue(lines.contains("FERCHO_TRACE rounds=2 tools=get_cart"));
        assertFalse(lines.stream().anyMatch(line -> line.contains("boom")));
    }

    @Test
    void shouldLogConfirmationRequiredToolExecution() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall(
                "c1", "cancel_order", Map.of("orderId", ORDER_ID.toString())))));
        llm.enqueue(LlmResponse.text("Â¿Confirmas la cancelaciÃ³n?"));

        ChatResponse response = chat.execute(new ChatCommand(null, "cancela mi pedido", null));

        assertTrue(response.awaitingConfirmation());
        List<String> lines = ferchoLines();
        assertTrue(lines.contains("FERCHO_TOOL round=1 tool=cancel_order status=CONFIRMATION_REQUIRED"));
        assertTrue(lines.contains("FERCHO_TRACE rounds=2 tools=cancel_order"));
        assertFalse(lines.stream().anyMatch(line -> line.contains(ORDER_ID.toString())));
    }

    @Test
    void shouldLogMultipleRoundsAndToolTrace() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall("c1", "get_cart", Map.of()))));
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall("c2", "get_cart", Map.of()))));
        llm.enqueue(LlmResponse.text("Listo."));

        chat.execute(new ChatCommand(null, "ver carrito dos veces", null));

        List<String> lines = ferchoLines();
        assertTrue(lines.contains("FERCHO_TOOL round=1 tool=get_cart status=SUCCESS"));
        assertTrue(lines.contains("FERCHO_TOOL round=2 tool=get_cart status=SUCCESS"));
        assertTrue(lines.contains("FERCHO_TRACE rounds=3 tools=get_cart,get_cart"));
    }
}
