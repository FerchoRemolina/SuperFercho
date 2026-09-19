package com.superfercho.assistant.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.confirmation.SensitiveActionType;
import com.superfercho.assistant.application.dto.chat.ChatCommand;
import com.superfercho.assistant.application.dto.chat.ChatResponse;
import com.superfercho.assistant.application.dto.chat.ExplicitConfirmation;
import com.superfercho.assistant.application.dto.llm.LlmMessage;
import com.superfercho.assistant.application.dto.llm.LlmResponse;
import com.superfercho.assistant.application.exception.InvalidConfirmationException;
import com.superfercho.assistant.application.port.out.ClockPort;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolRegistry;
import com.superfercho.assistant.application.tool.catalog.GetProductTool;
import com.superfercho.assistant.application.tool.knowledge.SearchKnowledgeTool;
import com.superfercho.assistant.application.tool.orders.CancelOrderTool;
import com.superfercho.assistant.application.tool.orders.CheckoutTool;
import com.superfercho.assistant.application.tool.shopping.GetCartTool;
import com.superfercho.assistant.domain.model.MessageRole;
import com.superfercho.assistant.infrastructure.confirmation.InMemoryPendingSensitiveActionStore;
import com.superfercho.assistant.infrastructure.conversation.InMemoryConversationStore;
import com.superfercho.assistant.infrastructure.llm.FakeLlmAdapter;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.usecase.GetProductUseCase;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import com.superfercho.knowledge.application.dto.KnowledgeSearchResult;
import com.superfercho.knowledge.application.dto.SearchKnowledgeCommand;
import com.superfercho.knowledge.application.usecase.SearchKnowledgeUseCase;
import com.superfercho.orders.application.dto.CancelOrderCommand;
import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.dto.ShippingAddressResult;
import com.superfercho.orders.application.port.in.CancelOrderUseCase;
import com.superfercho.orders.application.port.in.CheckoutUseCase;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.cart.CartItemResponse;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import com.superfercho.shopping.domain.model.CartStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-18T15:00:00Z");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CART_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ADDRESS_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID ORDER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID ITEM_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ClockPort clockPort;

    @Mock
    private GetCartUseCase getCartUseCase;

    @Mock
    private GetProductUseCase getProductUseCase;

    @Mock
    private CheckoutUseCase checkoutUseCase;

    @Mock
    private CancelOrderUseCase cancelOrderUseCase;

    @Mock
    private SearchKnowledgeUseCase searchKnowledgeUseCase;

    private FakeLlmAdapter llm;
    private InMemoryConversationStore conversations;
    private InMemoryPendingSensitiveActionStore pending;
    private ChatApplicationService chat;

    @BeforeEach
    void setUp() {
        llm = new FakeLlmAdapter();
        conversations = new InMemoryConversationStore();
        pending = new InMemoryPendingSensitiveActionStore();
        List<AssistantTool> tools = List.of(
                new GetCartTool(getCartUseCase),
                new GetProductTool(getProductUseCase),
                new SearchKnowledgeTool(searchKnowledgeUseCase),
                new CheckoutTool(currentUserProvider, getCartUseCase, getProductUseCase, pending),
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
    }

    @Test
    void shouldReturnTextualLlmResponseForAuthenticatedUser() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        llm.enqueue(LlmResponse.text("Hola, ¿en qué te ayudo?"));

        ChatResponse response = chat.execute(new ChatCommand(null, "hola", null));

        assertEquals("Hola, ¿en qué te ayudo?", response.assistantMessage());
        assertEquals(USER_ID, conversations.findById(response.conversationId()).orElseThrow().userId());
        assertFalse(response.awaitingConfirmation());
        assertEquals(1, llm.requests().size());
        assertEquals("hola", llm.requests().get(0).messages().get(0).content());
    }

    @Test
    void shouldExecuteToolThenAskLlmAgain() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getCartUseCase.execute()).thenReturn(emptyCart());
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall("c1", ToolNames.GET_CART, Map.of()))));
        llm.enqueue(LlmResponse.text("Tu carrito está vacío."));

        ChatResponse response = chat.execute(new ChatCommand(null, "ver carrito", null));

        assertEquals("Tu carrito está vacío.", response.assistantMessage());
        verify(getCartUseCase).execute();
        assertEquals(2, llm.requests().size());
        assertEquals(MessageRole.TOOL, llm.requests().get(1).messages().get(2).role());
    }

    @Test
    void shouldExecuteMultipleToolCalls() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getCartUseCase.execute()).thenReturn(emptyCart());
        when(searchKnowledgeUseCase.execute(new SearchKnowledgeCommand("pollo", 5)))
                .thenReturn(new KnowledgeSearchResult(List.of()));
        llm.enqueue(LlmResponse.toolCalls(List.of(
                new LlmMessage.LlmToolCall("c1", ToolNames.GET_CART, Map.of()),
                new LlmMessage.LlmToolCall("c2", ToolNames.SEARCH_KNOWLEDGE, Map.of("query", "pollo")))));
        llm.enqueue(LlmResponse.text("Listo."));

        ChatResponse response = chat.execute(new ChatCommand(null, "carrito y receta", null));

        assertEquals("Listo.", response.assistantMessage());
        verify(getCartUseCase).execute();
        verify(searchKnowledgeUseCase).execute(new SearchKnowledgeCommand("pollo", 5));
    }

    @Test
    void shouldReturnToolFailureWhenToolIsNotAllowed() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        llm.enqueue(LlmResponse.toolCalls(
                List.of(new LlmMessage.LlmToolCall("c1", "delete_database", Map.of()))));
        llm.enqueue(LlmResponse.text("No puedo hacer eso."));

        ChatResponse response = chat.execute(new ChatCommand(null, "borra todo", null));

        assertEquals("No puedo hacer eso.", response.assistantMessage());
        String toolContent = llm.requests().get(1).messages().get(2).content();
        assertTrue(toolContent.contains("not allowed"));
    }

    @Test
    void shouldReturnToolFailureWhenArgumentsAreInvalid() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall(
                "c1", ToolNames.GET_CART, Map.of("userId", USER_ID.toString())))));
        llm.enqueue(LlmResponse.text("No puedo usar ese identificador."));

        ChatResponse response = chat.execute(new ChatCommand(null, "mi carrito", null));

        assertEquals("No puedo usar ese identificador.", response.assistantMessage());
        verify(getCartUseCase, never()).execute();
    }

    @Test
    void shouldPropagateUnauthenticatedUser() {
        when(currentUserProvider.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThrows(
                UnauthenticatedUserException.class, () -> chat.execute(new ChatCommand(null, "hola", null)));
    }

    @Test
    void checkoutWithoutConfirmationDoesNotExecuteCheckoutUseCase() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getCartUseCase.execute()).thenReturn(cartWithMilk());
        when(getProductUseCase.execute(any(GetProductCommand.class))).thenReturn(product());
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall(
                "c1",
                ToolNames.CHECKOUT,
                Map.of("addressId", ADDRESS_ID.toString(), "paymentMethod", "SIMULATED_CARD")))));
        llm.enqueue(LlmResponse.text("¿Confirmas la compra?"));

        ChatResponse response = chat.execute(new ChatCommand(null, "quiero pagar", null));

        assertTrue(response.awaitingConfirmation());
        assertEquals(SensitiveActionType.CHECKOUT, response.confirmationType());
        assertNotNull(response.confirmationToken());
        verify(checkoutUseCase, never()).execute(any());
    }

    @Test
    void confirmedCheckoutDelegatesToCheckoutUseCase() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getCartUseCase.execute()).thenReturn(cartWithMilk());
        when(getProductUseCase.execute(any(GetProductCommand.class))).thenReturn(product());
        when(checkoutUseCase.execute(any(CheckoutCommand.class)))
                .thenReturn(new CheckoutResult(ORDER_ID, "SF-1", OrderStatus.PENDING, PaymentStatus.APPROVED, Money.cop(new BigDecimal("10.50"))));
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall(
                "c1",
                ToolNames.CHECKOUT,
                Map.of("addressId", ADDRESS_ID.toString(), "paymentMethod", "SIMULATED_CARD")))));
        llm.enqueue(LlmResponse.text("¿Confirmas?"));

        ChatResponse prepared = chat.execute(new ChatCommand(null, "checkout", null));
        ChatResponse confirmed = chat.execute(new ChatCommand(
                prepared.conversationId(), null, new ExplicitConfirmation(prepared.confirmationToken())));

        assertTrue(confirmed.assistantMessage().contains("Checkout confirmed"));
        verify(checkoutUseCase).execute(any(CheckoutCommand.class));
        assertFalse(confirmed.awaitingConfirmation());
    }

    @Test
    void cancelWithoutConfirmationDoesNotExecuteCancelUseCase() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall(
                "c1", ToolNames.CANCEL_ORDER, Map.of("orderId", ORDER_ID.toString())))));
        llm.enqueue(LlmResponse.text("¿Confirmas la cancelación?"));

        ChatResponse response = chat.execute(new ChatCommand(null, "cancela el pedido", null));

        assertTrue(response.awaitingConfirmation());
        assertEquals(SensitiveActionType.CANCEL_ORDER, response.confirmationType());
        verify(cancelOrderUseCase, never()).execute(any());
    }

    @Test
    void confirmedCancellationDelegatesToCancelOrderUseCase() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(cancelOrderUseCase.execute(new CancelOrderCommand(ORDER_ID))).thenReturn(order());
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall(
                "c1", ToolNames.CANCEL_ORDER, Map.of("orderId", ORDER_ID.toString())))));
        llm.enqueue(LlmResponse.text("¿Confirmas?"));

        ChatResponse prepared = chat.execute(new ChatCommand(null, "cancelar", null));
        ChatResponse confirmed = chat.execute(new ChatCommand(
                prepared.conversationId(), null, new ExplicitConfirmation(prepared.confirmationToken())));

        assertTrue(confirmed.assistantMessage().contains("Cancellation confirmed"));
        verify(cancelOrderUseCase).execute(new CancelOrderCommand(ORDER_ID));
    }

    @Test
    void llmCannotConfirmSensitiveActionByItself() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getCartUseCase.execute()).thenReturn(cartWithMilk());
        when(getProductUseCase.execute(any(GetProductCommand.class))).thenReturn(product());
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall(
                "c1",
                ToolNames.CHECKOUT,
                Map.of("addressId", ADDRESS_ID.toString(), "paymentMethod", "SIMULATED_CARD")))));
        llm.enqueue(LlmResponse.text("¿Confirmas?"));

        ChatResponse prepared = chat.execute(new ChatCommand(null, "comprar", null));
        llm.enqueue(LlmResponse.text("Ok, ya lo pagué."));
        ChatResponse unconfirmed = chat.execute(new ChatCommand(prepared.conversationId(), "sí", null));

        verify(checkoutUseCase, never()).execute(any());
        assertEquals("Ok, ya lo pagué.", unconfirmed.assistantMessage());
    }

    @Test
    void shouldRejectUnknownConfirmationToken() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        llm.enqueue(LlmResponse.text("hola"));
        ChatResponse started = chat.execute(new ChatCommand(null, "hola", null));

        assertThrows(
                InvalidConfirmationException.class,
                () -> chat.execute(new ChatCommand(
                        started.conversationId(), null, new ExplicitConfirmation("missing-token"))));
        verify(checkoutUseCase, never()).execute(any());
        verify(cancelOrderUseCase, never()).execute(any());
    }

    private static CartResponse emptyCart() {
        return new CartResponse(CART_ID, USER_ID, CartStatus.ACTIVE, List.of(), NOW, NOW);
    }

    private static CartResponse cartWithMilk() {
        return new CartResponse(
                CART_ID,
                USER_ID,
                CartStatus.ACTIVE,
                List.of(new CartItemResponse(
                        ITEM_ID, PRODUCT_ID, 2, Money.cop(new BigDecimal("10.50")), NOW, NOW)),
                NOW,
                NOW);
    }

    private static ProductResult product() {
        return new ProductResult(
                PRODUCT_ID,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "7701",
                "Leche",
                "Alpina",
                "1L",
                Money.cop(new BigDecimal("10.50")),
                8,
                null,
                ProductStatus.ACTIVE,
                NOW,
                NOW);
    }

    private static OrderResult order() {
        return new OrderResult(
                ORDER_ID,
                "SF-1",
                OrderStatus.CANCELLED,
                List.of(),
                Money.cop(new BigDecimal("10.50")),
                Money.cop(new BigDecimal("10.50")),
                new ShippingAddressResult("Ada", "Calle 1", null, "Bogotá", "Cundinamarca", "300"),
                UUID.randomUUID(),
                NOW,
                null,
                NOW,
                NOW);
    }
}
