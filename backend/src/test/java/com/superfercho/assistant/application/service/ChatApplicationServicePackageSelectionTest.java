package com.superfercho.assistant.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.confirmation.ConfirmationFingerprints;
import com.superfercho.assistant.application.confirmation.PendingSensitiveAction;
import com.superfercho.assistant.application.confirmation.SensitiveActionType;
import com.superfercho.assistant.application.dto.chat.ChatCommand;
import com.superfercho.assistant.application.dto.chat.ChatResponse;
import com.superfercho.assistant.application.dto.chat.ExplicitConfirmation;
import com.superfercho.assistant.application.dto.chat.PackageSelection;
import com.superfercho.assistant.application.dto.llm.LlmMessage;
import com.superfercho.assistant.application.dto.llm.LlmResponse;
import com.superfercho.assistant.application.exception.InvalidChatRequestException;
import com.superfercho.assistant.application.exception.InvalidPackageSelectionException;
import com.superfercho.assistant.application.fercho.FerchoLimits;
import com.superfercho.assistant.application.fercho.FerchoTurnDecision;
import com.superfercho.assistant.application.fercho.FerchoUsageGate;
import com.superfercho.assistant.application.packagesel.PackageSelectionDurations;
import com.superfercho.assistant.application.packagesel.PackageSelectionOption;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposal;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposalStatus;
import com.superfercho.assistant.application.port.out.ClockPort;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolRegistry;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.assistant.domain.model.Conversation;
import com.superfercho.assistant.infrastructure.confirmation.InMemoryPendingSensitiveActionStore;
import com.superfercho.assistant.infrastructure.conversation.InMemoryConversationStore;
import com.superfercho.assistant.infrastructure.llm.FakeLlmAdapter;
import com.superfercho.assistant.infrastructure.packagesel.InMemoryPackageSelectionProposalStore;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.usecase.GetProductUseCase;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.port.in.CancelOrderUseCase;
import com.superfercho.orders.application.port.in.CheckoutUseCase;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.cart.IdempotentAddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.IdempotentCartAddResult;
import com.superfercho.shopping.application.port.in.ApplyIdempotentCartAddUseCase;
import com.superfercho.shopping.domain.model.CartStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatApplicationServicePackageSelectionTest {

    private static final Instant NOW = Instant.parse("2026-10-23T18:00:00Z");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID OTHER_PRODUCT = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CART_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ADDRESS_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ClockPort clockPort;

    @Mock
    private GetProductUseCase getProductUseCase;

    @Mock
    private ApplyIdempotentCartAddUseCase applyIdempotentCartAddUseCase;

    @Mock
    private CheckoutUseCase checkoutUseCase;

    @Mock
    private CancelOrderUseCase cancelOrderUseCase;

    @Mock
    private FerchoUsageGate ferchoUsageGate;

    private FakeLlmAdapter llm;
    private InMemoryConversationStore conversations;
    private InMemoryPendingSensitiveActionStore pending;
    private InMemoryPackageSelectionProposalStore proposals;
    private ChatApplicationService chat;

    @BeforeEach
    void setUp() {
        llm = new FakeLlmAdapter();
        conversations = new InMemoryConversationStore();
        pending = new InMemoryPendingSensitiveActionStore();
        proposals = new InMemoryPackageSelectionProposalStore();
        Mockito.lenient()
                .when(ferchoUsageGate.evaluateAndReserve(any(), any(), any()))
                .thenReturn(FerchoTurnDecision.accepted(UUID.randomUUID()));
        Mockito.lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        Mockito.lenient().when(clockPort.currentTime()).thenReturn(NOW);
        Mockito.lenient()
                .when(applyIdempotentCartAddUseCase.replayIfApplied(any(), anyInt()))
                .thenReturn(Optional.empty());
        chat = new ChatApplicationService(
                currentUserProvider,
                clockPort,
                conversations,
                pending,
                llm,
                new ToolRegistry(List.of()),
                checkoutUseCase,
                cancelOrderUseCase,
                ferchoUsageGate,
                new FerchoLimits(30, Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofHours(24)),
                proposals,
                applyIdempotentCartAddUseCase,
                getProductUseCase);
    }

    @Test
    void exactOfferedSelectionAddsOnceWithoutLlm() {
        PackageSelectionProposal proposal = offer(2, 3);
        when(getProductUseCase.execute(any(GetProductCommand.class))).thenReturn(product(8));
        when(applyIdempotentCartAddUseCase.execute(any()))
                .thenReturn(IdempotentCartAddResult.applied(cart()));

        ChatResponse response = chat.execute(new ChatCommand(
                null, null, null, new PackageSelection(proposal.proposalId(), 2)));

        assertTrue(response.assistantMessage().contains("2 paquete"));
        assertNull(response.packageProposal());
        assertEquals(0, llm.requests().size());
        verify(applyIdempotentCartAddUseCase, times(1))
                .execute(new IdempotentAddProductToCartCommand(proposal.proposalId(), PRODUCT_ID, 2));
        assertEquals(
                PackageSelectionProposalStatus.CONSUMED,
                proposals.findById(proposal.proposalId(), NOW).orElseThrow().status());
    }

    @Test
    void inexactAllowedCountAddsAndDisallowedCountIsRejected() {
        PackageSelectionProposal proposal = offer(2, 3);
        when(getProductUseCase.execute(any(GetProductCommand.class))).thenReturn(product(8));
        when(applyIdempotentCartAddUseCase.execute(any()))
                .thenReturn(IdempotentCartAddResult.applied(cart()));

        assertThrows(
                InvalidPackageSelectionException.class,
                () -> chat.execute(new ChatCommand(
                        null, null, null, new PackageSelection(proposal.proposalId(), 4))));
        verify(applyIdempotentCartAddUseCase, never()).execute(any());

        ChatResponse response = chat.execute(new ChatCommand(
                null, null, null, new PackageSelection(proposal.proposalId(), 3)));
        assertTrue(response.assistantMessage().contains("3 paquete"));
        verify(applyIdempotentCartAddUseCase)
                .execute(new IdempotentAddProductToCartCommand(proposal.proposalId(), PRODUCT_ID, 3));
    }

    @Test
    void genericYesDoesNotConsumeTheProposal() {
        PackageSelectionProposal proposal = offer(2, 3);
        llm.enqueue(LlmResponse.text("Dime cuántos paquetes quieres."));

        ChatResponse response = chat.execute(new ChatCommand(existingConversation(), "sí", null));

        assertEquals("Dime cuántos paquetes quieres.", response.assistantMessage());
        assertEquals(proposal.proposalId(), response.packageProposal().proposalId());
        verify(applyIdempotentCartAddUseCase, never()).execute(any());
        assertEquals(
                PackageSelectionProposalStatus.OFFERED,
                proposals.findById(proposal.proposalId(), NOW).orElseThrow().status());
    }

    @Test
    void newConversationMessageDoesNotReattachPreviousOffer() {
        PackageSelectionProposal proposal = offer(2, 3);
        llm.enqueue(LlmResponse.text("Elige paquetes"));
        ChatResponse sameConversation = chat.execute(new ChatCommand(existingConversation(), "¿cuántos?", null));
        assertEquals(proposal.proposalId(), sameConversation.packageProposal().proposalId());

        llm.enqueue(LlmResponse.text("Hola, ¿en qué te ayudo?"));
        ChatResponse fresh = chat.execute(new ChatCommand(null, "hola", null));

        assertNull(fresh.packageProposal());
        assertEquals(
                PackageSelectionProposalStatus.SUPERSEDED,
                proposals.findById(proposal.proposalId(), NOW).orElseThrow().status());
        assertTrue(proposals.findActiveOffered(USER_ID, NOW).isEmpty());
        verify(applyIdempotentCartAddUseCase, never()).execute(any());
    }

    @Test
    void unrelatedShoppingListTurnDetachesPreviousOffer() {
        PackageSelectionProposal proposal = offer(2, 3);
        chat = chatWith(new StubTool(ToolNames.LIST_SHOPPING_LISTS));
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall(
                "c1", ToolNames.LIST_SHOPPING_LISTS, Map.of()))));
        llm.enqueue(LlmResponse.text("Revisé tu mercado mensual y lo agregué al carrito."));

        ChatResponse response = chat.execute(new ChatCommand(
                existingConversation(), "ayuda con mi mercado mensual", null));

        assertNull(response.packageProposal());
        assertEquals(
                PackageSelectionProposalStatus.SUPERSEDED,
                proposals.findById(proposal.proposalId(), NOW).orElseThrow().status());
        assertThrows(
                InvalidPackageSelectionException.class,
                () -> chat.execute(new ChatCommand(
                        null, null, null, new PackageSelection(proposal.proposalId(), 2))));
        verify(applyIdempotentCartAddUseCase, never()).execute(any());
    }

    @Test
    void addingShoppingListToCartClearsPendingPotatoOffer() {
        PackageSelectionProposal potato = offer(3, 4);
        UUID conversationId = existingConversation();
        chat = chatWith(
                new StubTool(ToolNames.ADD_SHOPPING_LIST_TO_CART),
                new StubTool(ToolNames.GET_CART));
        llm.enqueue(LlmResponse.toolCalls(List.of(
                new LlmMessage.LlmToolCall("c1", ToolNames.ADD_SHOPPING_LIST_TO_CART, Map.of()),
                new LlmMessage.LlmToolCall("c2", ToolNames.GET_CART, Map.of()))));
        llm.enqueue(LlmResponse.text("Pasé tu mercado mensual al carrito. Subtotal 149900 COP."));

        ChatResponse response = chat.execute(new ChatCommand(
                conversationId, "quiero comprar mi mercado mensual", null));

        assertNull(response.packageProposal());
        assertEquals(
                PackageSelectionProposalStatus.SUPERSEDED,
                proposals.findById(potato.proposalId(), NOW).orElseThrow().status());
        assertTrue(proposals.findActiveOffered(USER_ID, NOW).isEmpty());
        assertThrows(
                InvalidPackageSelectionException.class,
                () -> chat.execute(new ChatCommand(
                        conversationId, null, null, new PackageSelection(potato.proposalId(), 3))));
        verify(applyIdempotentCartAddUseCase, never()).execute(any());
    }

    @Test
    void shoppingListToCartDetachesEvenIfQuantityEquivalenceReruns() {
        PackageSelectionProposal potato = offer(3, 4);
        UUID conversationId = existingConversation();
        AtomicReference<PackageSelectionProposal> rerun = new AtomicReference<>();
        chat = chatWith(
                new StubTool(ToolNames.ADD_SHOPPING_LIST_TO_CART),
                new StubTool(ToolNames.GET_CART),
                new StubTool(ToolNames.RESOLVE_QUANTITY_EQUIVALENCE, () -> {
                    PackageSelectionProposal again = newProposal(3, 4);
                    rerun.set(again);
                    proposals.replaceOffered(again, NOW);
                }));
        llm.enqueue(LlmResponse.toolCalls(List.of(
                new LlmMessage.LlmToolCall("c1", ToolNames.RESOLVE_QUANTITY_EQUIVALENCE, Map.of()),
                new LlmMessage.LlmToolCall("c2", ToolNames.ADD_SHOPPING_LIST_TO_CART, Map.of()),
                new LlmMessage.LlmToolCall("c3", ToolNames.GET_CART, Map.of()))));
        llm.enqueue(LlmResponse.text("Pasé tu mercado mensual al carrito. Subtotal 149900 COP."));

        ChatResponse response = chat.execute(new ChatCommand(
                conversationId, "quiero comprar mi mercado mensual", null));

        assertNull(response.packageProposal());
        assertEquals(
                PackageSelectionProposalStatus.SUPERSEDED,
                proposals.findById(potato.proposalId(), NOW).orElseThrow().status());
        assertEquals(
                PackageSelectionProposalStatus.SUPERSEDED,
                proposals.findById(rerun.get().proposalId(), NOW).orElseThrow().status());
        assertTrue(proposals.findActiveOffered(USER_ID, NOW).isEmpty());
        verify(applyIdempotentCartAddUseCase, never()).execute(any());
    }

    @Test
    void afterListDetachANewPotatoRequestCanOfferAgain() {
        PackageSelectionProposal potato = offer(3, 4);
        UUID conversationId = existingConversation();
        AtomicReference<PackageSelectionProposal> nextOffer = new AtomicReference<>();
        chat = chatWith(
                new StubTool(ToolNames.ADD_SHOPPING_LIST_TO_CART),
                new StubTool(ToolNames.RESOLVE_QUANTITY_EQUIVALENCE, () -> {
                    PackageSelectionProposal next = newProposal(1, 2);
                    nextOffer.set(next);
                    proposals.replaceOffered(next, NOW);
                }));
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall(
                "c1", ToolNames.ADD_SHOPPING_LIST_TO_CART, Map.of()))));
        llm.enqueue(LlmResponse.text("Pasé tu mercado mensual al carrito."));
        ChatResponse listTurn = chat.execute(new ChatCommand(
                conversationId, "quiero comprar mi mercado mensual", null));
        assertNull(listTurn.packageProposal());
        assertEquals(
                PackageSelectionProposalStatus.SUPERSEDED,
                proposals.findById(potato.proposalId(), NOW).orElseThrow().status());

        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall(
                "c2", ToolNames.RESOLVE_QUANTITY_EQUIVALENCE, Map.of()))));
        llm.enqueue(LlmResponse.text("Elige paquetes de Papa"));
        ChatResponse potatoTurn = chat.execute(new ChatCommand(
                conversationId, "quiero 1.8 kg de papa criolla", null));

        assertEquals(nextOffer.get().proposalId(), potatoTurn.packageProposal().proposalId());
        assertEquals(1, potatoTurn.packageProposal().options().get(0).packageCount());
        assertEquals(2, potatoTurn.packageProposal().options().get(1).packageCount());
        assertEquals(
                PackageSelectionProposalStatus.OFFERED,
                proposals.findById(nextOffer.get().proposalId(), NOW).orElseThrow().status());
    }

    @Test
    void catalogSearchWithNewQuantityOfferKeepsChips() {
        AtomicReference<PackageSelectionProposal> fresh = new AtomicReference<>();
        chat = chatWith(
                new StubTool(ToolNames.SEARCH_PRODUCTS),
                new StubTool(ToolNames.RESOLVE_QUANTITY_EQUIVALENCE, () -> {
                    PackageSelectionProposal next = newProposal(2, 3);
                    fresh.set(next);
                    proposals.replaceOffered(next, NOW);
                }));
        llm.enqueue(LlmResponse.toolCalls(List.of(
                new LlmMessage.LlmToolCall("c1", ToolNames.SEARCH_PRODUCTS, Map.of()),
                new LlmMessage.LlmToolCall("c2", ToolNames.RESOLVE_QUANTITY_EQUIVALENCE, Map.of()))));
        llm.enqueue(LlmResponse.text("Elige paquetes"));

        ChatResponse response = chat.execute(new ChatCommand(
                existingConversation(), "quiero 2.3 kg de arroz", null));

        assertEquals(fresh.get().proposalId(), response.packageProposal().proposalId());
        assertEquals(2, response.packageProposal().options().get(0).packageCount());
    }

    @Test
    void laterOfferForSameProductReplacesAttachedOptions() {
        PackageSelectionProposal first = offer(1, 2);
        PackageSelectionProposal second = offer(3, 4);
        llm.enqueue(LlmResponse.text("Elige paquetes"));

        ChatResponse response = chat.execute(new ChatCommand(existingConversation(), "quiero más papa", null));

        assertEquals(second.proposalId(), response.packageProposal().proposalId());
        assertEquals(3, response.packageProposal().options().get(0).packageCount());
        assertEquals(4, response.packageProposal().options().get(1).packageCount());
        assertEquals(
                PackageSelectionProposalStatus.SUPERSEDED,
                proposals.findById(first.proposalId(), NOW).orElseThrow().status());
        assertEquals(
                PackageSelectionProposalStatus.OFFERED,
                proposals.findById(second.proposalId(), NOW).orElseThrow().status());
    }

    @Test
    void expiredOfferIsNotAttachedAndCannotBeSelected() {
        PackageSelectionProposal proposal = offer(2, 3);
        Instant expiredAt = NOW.plus(PackageSelectionDurations.PROPOSAL_TTL);
        when(clockPort.currentTime()).thenReturn(expiredAt);
        llm.enqueue(LlmResponse.text("Hola"));

        ChatResponse response = chat.execute(new ChatCommand(existingConversation(), "hola", null));

        assertNull(response.packageProposal());
        assertTrue(proposals.findActiveOffered(USER_ID, expiredAt).isEmpty());
        assertThrows(
                InvalidPackageSelectionException.class,
                () -> chat.execute(new ChatCommand(
                        null, null, null, new PackageSelection(proposal.proposalId(), 2))));
        verify(applyIdempotentCartAddUseCase, never()).execute(any());
    }

    @Test
    void consumedProposalIsNotReattachedAndReplayDoesNotAddAgain() {
        PackageSelectionProposal proposal = offer(2, 3);
        when(getProductUseCase.execute(any(GetProductCommand.class))).thenReturn(product(8));
        when(applyIdempotentCartAddUseCase.execute(any()))
                .thenReturn(IdempotentCartAddResult.applied(cart()));
        ChatResponse consumed = chat.execute(new ChatCommand(
                null, null, null, new PackageSelection(proposal.proposalId(), 2)));
        when(applyIdempotentCartAddUseCase.replayIfApplied(proposal.proposalId(), 2))
                .thenReturn(Optional.of(IdempotentCartAddResult.replayed(cart())));
        llm.enqueue(LlmResponse.text("De nada"));

        ChatResponse later = chat.execute(new ChatCommand(consumed.conversationId(), "gracias", null));
        ChatResponse replay = chat.execute(new ChatCommand(
                consumed.conversationId(), null, null, new PackageSelection(proposal.proposalId(), 2)));

        assertNull(later.packageProposal());
        assertNull(replay.packageProposal());
        assertTrue(replay.assistantMessage().contains("2 paquete"));
        verify(applyIdempotentCartAddUseCase, times(1)).execute(any());
    }

    @Test
    void getCartFollowUpKeepsActiveOffer() {
        PackageSelectionProposal proposal = offer(2, 3);
        chat = chatWith(new StubTool(ToolNames.GET_CART));
        llm.enqueue(LlmResponse.toolCalls(List.of(new LlmMessage.LlmToolCall(
                "c1", ToolNames.GET_CART, Map.of()))));
        llm.enqueue(LlmResponse.text("Tu carrito tiene 1 artículo."));

        ChatResponse response = chat.execute(new ChatCommand(
                existingConversation(), "qué hay en el carrito", null));

        assertEquals(proposal.proposalId(), response.packageProposal().proposalId());
        assertEquals(
                PackageSelectionProposalStatus.OFFERED,
                proposals.findById(proposal.proposalId(), NOW).orElseThrow().status());
    }

    @Test
    void unknownProposalOrOtherUserIsRejected() {
        PackageSelectionProposal proposal = offer(2, 3);
        assertThrows(
                InvalidPackageSelectionException.class,
                () -> chat.execute(new ChatCommand(
                        null, null, null, new PackageSelection(UUID.randomUUID(), 2))));

        when(currentUserProvider.getCurrentUserId()).thenReturn(OTHER_USER);
        assertThrows(
                InvalidPackageSelectionException.class,
                () -> chat.execute(new ChatCommand(
                        null, null, null, new PackageSelection(proposal.proposalId(), 2))));
        verify(applyIdempotentCartAddUseCase, never()).execute(any());
        assertEquals(
                PackageSelectionProposalStatus.OFFERED,
                proposals.findById(proposal.proposalId(), NOW).orElseThrow().status());
    }

    @Test
    void expiredAndSupersededSelectionsAreRejected() {
        PackageSelectionProposal expired = offer(2, 3);
        when(clockPort.currentTime()).thenReturn(NOW.plus(PackageSelectionDurations.PROPOSAL_TTL));
        assertThrows(
                InvalidPackageSelectionException.class,
                () -> chat.execute(new ChatCommand(
                        null, null, null, new PackageSelection(expired.proposalId(), 2))));

        when(clockPort.currentTime()).thenReturn(NOW);
        PackageSelectionProposal first = offer(2, 3);
        offer(2, 3);
        assertThrows(
                InvalidPackageSelectionException.class,
                () -> chat.execute(new ChatCommand(
                        null, null, null, new PackageSelection(first.proposalId(), 2))));
        verify(applyIdempotentCartAddUseCase, never()).execute(any());
    }

    @Test
    void consumedSelectionWithReceiptReplaysSuccessfully() {
        PackageSelectionProposal proposal = offer(2, 3);
        when(getProductUseCase.execute(any(GetProductCommand.class))).thenReturn(product(8));
        when(applyIdempotentCartAddUseCase.execute(any()))
                .thenReturn(IdempotentCartAddResult.applied(cart()));
        chat.execute(new ChatCommand(null, null, null, new PackageSelection(proposal.proposalId(), 2)));
        when(applyIdempotentCartAddUseCase.replayIfApplied(proposal.proposalId(), 2))
                .thenReturn(Optional.of(IdempotentCartAddResult.replayed(cart())));

        ChatResponse replay = chat.execute(new ChatCommand(
                null, null, null, new PackageSelection(proposal.proposalId(), 2)));

        assertTrue(replay.assistantMessage().contains("2 paquete"));
        assertNull(replay.packageProposal());
        verify(applyIdempotentCartAddUseCase, times(1)).execute(any());
    }

    @Test
    void restartedStoreWithReceiptReplaysWithoutAdding() {
        UUID proposalId = UUID.randomUUID();
        when(applyIdempotentCartAddUseCase.replayIfApplied(proposalId, 2))
                .thenReturn(Optional.of(IdempotentCartAddResult.replayed(cart())));

        ChatResponse response = chat.execute(new ChatCommand(
                null, null, null, new PackageSelection(proposalId, 2)));

        assertTrue(response.assistantMessage().contains("ya estaban en el carrito"));
        verify(applyIdempotentCartAddUseCase, never()).execute(any());
        verify(getProductUseCase, never()).execute(any());
    }

    @Test
    void recoveredOfferedProposalAfterCommitDoesNotAddAgain() {
        PackageSelectionProposal proposal = offer(2, 3);
        when(getProductUseCase.execute(any(GetProductCommand.class))).thenReturn(product(8));
        when(applyIdempotentCartAddUseCase.execute(any()))
                .thenReturn(IdempotentCartAddResult.applied(cart()))
                .thenReturn(IdempotentCartAddResult.replayed(cart()));
        InMemoryPackageSelectionProposalStore spy = Mockito.spy(proposals);
        doReturn(false).when(spy).tryMarkConsumed(any(), any(), any());
        chat = new ChatApplicationService(
                currentUserProvider,
                clockPort,
                conversations,
                pending,
                llm,
                new ToolRegistry(List.of()),
                checkoutUseCase,
                cancelOrderUseCase,
                ferchoUsageGate,
                new FerchoLimits(30, Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofHours(24)),
                spy,
                applyIdempotentCartAddUseCase,
                getProductUseCase);
        spy.replaceOffered(proposal, NOW);

        chat.execute(new ChatCommand(null, null, null, new PackageSelection(proposal.proposalId(), 2)));
        Instant afterLease = NOW.plus(PackageSelectionDurations.CONSUMING_LEASE).plusSeconds(1);
        when(clockPort.currentTime()).thenReturn(afterLease);
        ChatResponse retry = chat.execute(new ChatCommand(
                null, null, null, new PackageSelection(proposal.proposalId(), 2)));

        assertTrue(retry.assistantMessage().contains("2 paquete"));
        verify(applyIdempotentCartAddUseCase, times(2))
                .execute(new IdempotentAddProductToCartCommand(proposal.proposalId(), PRODUCT_ID, 2));
    }

    @Test
    void presentationChangeUnavailableSkuAndInsufficientStockRestoreOffered() {
        PackageSelectionProposal proposal = offer(2, 3);
        when(getProductUseCase.execute(any(GetProductCommand.class)))
                .thenReturn(productWithPresentation(Presentation.of(500, PresentationUnit.G), 8));
        InvalidPackageSelectionException presentation =
                assertThrows(
                        InvalidPackageSelectionException.class,
                        () -> chat.execute(new ChatCommand(
                                null, null, null, new PackageSelection(proposal.proposalId(), 2))));
        assertTrue(presentation.getMessage().contains("presentation"));
        assertEquals(
                PackageSelectionProposalStatus.OFFERED,
                proposals.findById(proposal.proposalId(), NOW).orElseThrow().status());

        when(getProductUseCase.execute(any(GetProductCommand.class)))
                .thenThrow(new ProductNotFoundException(PRODUCT_ID));
        assertThrows(
                InvalidPackageSelectionException.class,
                () -> chat.execute(new ChatCommand(
                        null, null, null, new PackageSelection(proposal.proposalId(), 2))));
        assertEquals(
                PackageSelectionProposalStatus.OFFERED,
                proposals.findById(proposal.proposalId(), NOW).orElseThrow().status());

        when(getProductUseCase.execute(any(GetProductCommand.class))).thenReturn(product(1));
        assertThrows(
                InvalidPackageSelectionException.class,
                () -> chat.execute(new ChatCommand(
                        null, null, null, new PackageSelection(proposal.proposalId(), 2))));
        verify(applyIdempotentCartAddUseCase, never()).execute(any());
        assertEquals(
                PackageSelectionProposalStatus.OFFERED,
                proposals.findById(proposal.proposalId(), NOW).orElseThrow().status());
    }

    @Test
    void failedCartAddRestoresOfferedProposal() {
        PackageSelectionProposal proposal = offer(2, 3);
        when(getProductUseCase.execute(any(GetProductCommand.class))).thenReturn(product(8));
        when(applyIdempotentCartAddUseCase.execute(any())).thenThrow(new IllegalStateException("cart unavailable"));

        assertThrows(
                InvalidPackageSelectionException.class,
                () -> chat.execute(new ChatCommand(
                        null, null, null, new PackageSelection(proposal.proposalId(), 2))));
        assertEquals(
                PackageSelectionProposalStatus.OFFERED,
                proposals.findById(proposal.proposalId(), NOW).orElseThrow().status());
    }

    @Test
    void concurrentSelectionsAddAtMostOnce() throws InterruptedException {
        PackageSelectionProposal proposal = offer(2, 3);
        when(getProductUseCase.execute(any(GetProductCommand.class))).thenReturn(product(8));
        when(applyIdempotentCartAddUseCase.execute(any()))
                .thenReturn(IdempotentCartAddResult.applied(cart()));
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        Runnable task = () -> {
            await(start);
            try {
                chat.execute(new ChatCommand(
                        null, null, null, new PackageSelection(proposal.proposalId(), 2)));
                successes.incrementAndGet();
            } catch (InvalidPackageSelectionException ignored) {
                failures.incrementAndGet();
            }
        };
        Thread first = new Thread(task);
        Thread second = new Thread(task);
        first.start();
        second.start();
        start.countDown();
        first.join();
        second.join();
        assertEquals(1, successes.get());
        assertEquals(1, failures.get());
        verify(applyIdempotentCartAddUseCase, times(1))
                .execute(new IdempotentAddProductToCartCommand(proposal.proposalId(), PRODUCT_ID, 2));
    }

    @Test
    void failedConsumedCasAfterSuccessfulAddDoesNotRepeatTheAdd() {
        InMemoryPackageSelectionProposalStore real = new InMemoryPackageSelectionProposalStore();
        InMemoryPackageSelectionProposalStore spy = Mockito.spy(real);
        doReturn(false).when(spy).tryMarkConsumed(any(), any(), any());
        chat = new ChatApplicationService(
                currentUserProvider,
                clockPort,
                conversations,
                pending,
                llm,
                new ToolRegistry(List.of()),
                checkoutUseCase,
                cancelOrderUseCase,
                ferchoUsageGate,
                new FerchoLimits(30, Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofHours(24)),
                spy,
                applyIdempotentCartAddUseCase,
                getProductUseCase);
        PackageSelectionProposal proposal = newProposal(2, 3);
        spy.replaceOffered(proposal, NOW);
        when(getProductUseCase.execute(any(GetProductCommand.class))).thenReturn(product(8));
        when(applyIdempotentCartAddUseCase.execute(any()))
                .thenReturn(IdempotentCartAddResult.applied(cart()));

        ChatResponse response = chat.execute(new ChatCommand(
                null, null, null, new PackageSelection(proposal.proposalId(), 2)));

        assertTrue(response.assistantMessage().contains("2 paquete"));
        verify(applyIdempotentCartAddUseCase, times(1))
                .execute(new IdempotentAddProductToCartCommand(proposal.proposalId(), PRODUCT_ID, 2));
        verify(spy, times(3)).tryMarkConsumed(proposal.proposalId(), USER_ID, NOW);
    }

    @Test
    void sensitivePendingBlocksPackageSelectionWithoutTouchingThePendingStore() {
        PackageSelectionProposal proposal = offer(2, 3);
        CheckoutCommand checkoutCommand =
                new CheckoutCommand(ADDRESS_ID, PaymentMethod.SIMULATED_CARD, List.of(), "idem-1");
        pending.save(PendingSensitiveAction.checkout(
                "tok",
                USER_ID,
                ConfirmationFingerprints.checkout(checkoutCommand),
                checkoutCommand));

        assertThrows(
                InvalidPackageSelectionException.class,
                () -> chat.execute(new ChatCommand(
                        null, null, null, new PackageSelection(proposal.proposalId(), 2))));
        assertTrue(pending.findByToken("tok").isPresent());
        verify(checkoutUseCase, never()).execute(any());
        verify(applyIdempotentCartAddUseCase, never()).execute(any());
    }

    @Test
    void combinedChannelsAreRejectedAndCheckoutStillConfirms() {
        assertThrows(
                InvalidChatRequestException.class,
                () -> chat.execute(new ChatCommand(
                        null,
                        null,
                        new ExplicitConfirmation("tok"),
                        new PackageSelection(UUID.randomUUID(), 2))));
        assertThrows(
                InvalidChatRequestException.class,
                () -> chat.execute(new ChatCommand(
                        null, "sí", null, new PackageSelection(UUID.randomUUID(), 2))));

        CheckoutCommand checkoutCommand =
                new CheckoutCommand(ADDRESS_ID, PaymentMethod.SIMULATED_CARD, List.of(), "idem-1");
        pending.save(PendingSensitiveAction.checkout(
                "tok",
                USER_ID,
                ConfirmationFingerprints.checkout(checkoutCommand),
                checkoutCommand));
        when(checkoutUseCase.execute(any()))
                .thenReturn(new CheckoutResult(
                        UUID.randomUUID(),
                        "SF-1",
                        OrderStatus.CONFIRMED,
                        PaymentStatus.APPROVED,
                        Money.cop(new BigDecimal("10.50"))));

        ChatResponse confirmed = chat.execute(new ChatCommand(null, null, new ExplicitConfirmation("tok")));
        assertTrue(confirmed.assistantMessage().toLowerCase().contains("pedido")
                || confirmed.assistantMessage().contains("SF-1"));
        verify(checkoutUseCase).execute(any());
        verify(applyIdempotentCartAddUseCase, never()).execute(any());
    }

    private UUID existingConversation() {
        return conversations.save(Conversation.start(UUID.randomUUID(), USER_ID, NOW)).id();
    }

    private ChatApplicationService chatWith(AssistantTool... tools) {
        return new ChatApplicationService(
                currentUserProvider,
                clockPort,
                conversations,
                pending,
                llm,
                new ToolRegistry(List.of(tools)),
                checkoutUseCase,
                cancelOrderUseCase,
                ferchoUsageGate,
                new FerchoLimits(30, Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofHours(24)),
                proposals,
                applyIdempotentCartAddUseCase,
                getProductUseCase);
    }

    private PackageSelectionProposal offer(int firstCount, int secondCount) {
        PackageSelectionProposal proposal = newProposal(firstCount, secondCount);
        proposals.replaceOffered(proposal, NOW);
        return proposal;
    }

    private static final class StubTool implements AssistantTool {
        private final String name;
        private final Runnable onExecute;

        private StubTool(String name) {
            this(name, null);
        }

        private StubTool(String name, Runnable onExecute) {
            this.name = name;
            this.onExecute = onExecute;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return name;
        }

        @Override
        public ToolSchema schema() {
            return ToolSchema.of();
        }

        @Override
        public ToolRisk risk() {
            return ToolRisk.QUERY;
        }

        @Override
        public ToolResult execute(ToolArguments arguments) {
            if (onExecute != null) {
                onExecute.run();
            }
            return ToolResult.success("ok");
        }
    }

    private static PackageSelectionProposal newProposal(int firstCount, int secondCount) {
        return new PackageSelectionProposal(
                UUID.randomUUID(),
                USER_ID,
                PRODUCT_ID,
                "Arroz",
                new BigDecimal("1.000"),
                PresentationUnit.KG,
                List.of(
                        new PackageSelectionOption(firstCount, new BigDecimal("2000.000"), PresentationUnit.G),
                        new PackageSelectionOption(secondCount, new BigDecimal("3000.000"), PresentationUnit.G)),
                new BigDecimal("2.300"),
                PresentationUnit.KG,
                PackageSelectionProposalStatus.OFFERED,
                NOW,
                NOW.plus(PackageSelectionDurations.PROPOSAL_TTL),
                null);
    }

    private static ProductResult product(int stock) {
        return productWithPresentation(Presentation.of(1, PresentationUnit.KG), stock);
    }

    private static ProductResult productWithPresentation(Presentation presentation, int stock) {
        return new ProductResult(
                PRODUCT_ID,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                null,
                presentation,
                "7701",
                "Arroz",
                "Diana",
                "1KG",
                Money.cop(new BigDecimal("4500.00")),
                stock,
                null,
                ProductStatus.ACTIVE,
                NOW,
                NOW);
    }

    private static CartResponse cart() {
        return CartResponse.of(CART_ID, USER_ID, CartStatus.ACTIVE, List.of(), NOW, NOW);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
