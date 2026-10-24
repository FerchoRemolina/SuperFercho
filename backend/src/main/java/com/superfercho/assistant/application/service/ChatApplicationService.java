package com.superfercho.assistant.application.service;

import com.superfercho.assistant.application.confirmation.ConfirmationFingerprints;
import com.superfercho.assistant.application.confirmation.PendingSensitiveAction;
import com.superfercho.assistant.application.confirmation.SensitiveActionType;
import com.superfercho.assistant.application.dto.chat.ChatCommand;
import com.superfercho.assistant.application.dto.chat.ChatResponse;
import com.superfercho.assistant.application.dto.chat.ExplicitConfirmation;
import com.superfercho.assistant.application.dto.chat.PackageProposalView;
import com.superfercho.assistant.application.dto.chat.PackageSelection;
import com.superfercho.assistant.application.dto.llm.LlmMessage;
import com.superfercho.assistant.application.dto.llm.LlmRequest;
import com.superfercho.assistant.application.dto.llm.LlmResponse;
import com.superfercho.assistant.application.dto.llm.LlmToolDefinition;
import com.superfercho.assistant.application.dto.llm.LlmUsage;
import com.superfercho.assistant.application.exception.FerchoPausedException;
import com.superfercho.assistant.application.fercho.FerchoLimits;
import com.superfercho.assistant.application.fercho.FerchoTurnDecision;
import com.superfercho.assistant.application.fercho.FerchoTurnDecisionType;
import com.superfercho.assistant.application.fercho.FerchoUsageGate;
import com.superfercho.assistant.application.exception.ConversationNotFoundException;
import com.superfercho.assistant.application.exception.InvalidChatRequestException;
import com.superfercho.assistant.application.exception.InvalidConfirmationException;
import com.superfercho.assistant.application.exception.InvalidPackageSelectionException;
import com.superfercho.assistant.application.exception.ToolAccessDeniedException;
import com.superfercho.assistant.application.packagesel.PackageProposalRelevance;
import com.superfercho.assistant.application.packagesel.PackageProposalViews;
import com.superfercho.assistant.application.packagesel.PackageSelectionDurations;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposal;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposalStatus;
import com.superfercho.assistant.application.port.in.ChatUseCase;
import com.superfercho.assistant.application.port.out.ClockPort;
import com.superfercho.assistant.application.port.out.ConversationStore;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.port.out.LLMPort;
import com.superfercho.assistant.application.port.out.PackageSelectionProposalStore;
import com.superfercho.assistant.application.port.out.PendingSensitiveActionStore;
import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolAccess;
import com.superfercho.assistant.application.tool.ToolRegistry;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.domain.model.Conversation;
import com.superfercho.assistant.domain.model.Message;
import com.superfercho.assistant.domain.model.MessageRole;
import com.superfercho.assistant.domain.model.MessageToolCall;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.usecase.GetProductUseCase;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.orders.application.dto.CancelOrderCommand;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.port.in.CancelOrderUseCase;
import com.superfercho.orders.application.port.in.CheckoutUseCase;
import com.superfercho.shopping.application.dto.cart.IdempotentAddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.IdempotentCartAddResult;
import com.superfercho.shopping.application.exception.CartAddOperationConflictException;
import com.superfercho.shopping.application.port.in.ApplyIdempotentCartAddUseCase;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChatApplicationService implements ChatUseCase {

    private static final int MAX_TOOL_ROUNDS = 8;

    private static final int CONSUMED_CAS_RETRIES = 3;

    private static final Logger log = LoggerFactory.getLogger(ChatApplicationService.class);

    private final CurrentUserProvider currentUserProvider;
    private final ClockPort clockPort;
    private final ConversationStore conversationStore;
    private final PendingSensitiveActionStore pendingSensitiveActionStore;
    private final LLMPort llmPort;
    private final ToolRegistry toolRegistry;
    private final CheckoutUseCase checkoutUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final FerchoUsageGate ferchoUsageGate;
    private final FerchoLimits ferchoLimits;
    private final PackageSelectionProposalStore packageSelectionProposalStore;
    private final ApplyIdempotentCartAddUseCase applyIdempotentCartAddUseCase;
    private final GetProductUseCase getProductUseCase;

    public ChatApplicationService(
            CurrentUserProvider currentUserProvider,
            ClockPort clockPort,
            ConversationStore conversationStore,
            PendingSensitiveActionStore pendingSensitiveActionStore,
            LLMPort llmPort,
            ToolRegistry toolRegistry,
            CheckoutUseCase checkoutUseCase,
            CancelOrderUseCase cancelOrderUseCase,
            FerchoUsageGate ferchoUsageGate,
            FerchoLimits ferchoLimits,
            PackageSelectionProposalStore packageSelectionProposalStore,
            ApplyIdempotentCartAddUseCase applyIdempotentCartAddUseCase,
            GetProductUseCase getProductUseCase) {
        this.currentUserProvider = currentUserProvider;
        this.clockPort = clockPort;
        this.conversationStore = conversationStore;
        this.pendingSensitiveActionStore = pendingSensitiveActionStore;
        this.llmPort = llmPort;
        this.toolRegistry = toolRegistry;
        this.checkoutUseCase = checkoutUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.ferchoUsageGate = ferchoUsageGate;
        this.ferchoLimits = ferchoLimits;
        this.packageSelectionProposalStore = packageSelectionProposalStore;
        this.applyIdempotentCartAddUseCase = applyIdempotentCartAddUseCase;
        this.getProductUseCase = getProductUseCase;
    }

    @Override
    public ChatResponse execute(ChatCommand command) {
        if (command == null) {
            throw new InvalidChatRequestException("command cannot be null");
        }
        UUID userId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        // RestricciÃƒÂ³n de Fercho: se evalÃƒÂºa y registra ANTES de invocar la IA o
        // ejecutar herramientas (fail-closed ante fallo de persistencia).
        FerchoTurnDecision decision = ferchoUsageGate.evaluateAndReserve(userId, ferchoLimits, now);
        if (!decision.accepted()) {
            throw new FerchoPausedException(
                    decision.type() == FerchoTurnDecisionType.SUSPENDED
                            ? "FERCHO_SUSPENDED"
                            : "FERCHO_PAUSED",
                    pausedMessage(),
                    Math.max(0, Duration.between(now, decision.pausedUntil()).getSeconds()),
                    decision.pausedUntil());
        }
        Conversation conversation = loadOrStart(command.conversationId(), userId, now);
        boolean hasMessage = command.message() != null && !command.message().isBlank();
        boolean hasConfirmation = command.confirmation() != null;
        boolean hasPackageSelection = command.packageSelection() != null;
        if (hasConfirmation && hasPackageSelection) {
            throw new InvalidChatRequestException("a turn cannot combine confirmation and packageSelection");
        }
        if (hasPackageSelection && hasMessage) {
            throw new InvalidChatRequestException("a turn cannot combine message and packageSelection");
        }
        if (hasConfirmation) {
            return confirm(conversation, userId, command.confirmation(), now);
        }
        if (hasPackageSelection) {
            return applyPackageSelection(
                    conversation, userId, command.packageSelection(), now, decision.turnId());
        }
        if (!hasMessage) {
            throw new InvalidChatRequestException("message cannot be blank");
        }
        if (command.conversationId() == null) {
            packageSelectionProposalStore.supersedeActiveOffered(userId, now);
        }
        conversation = conversationStore.save(conversation.append(Message.user(UUID.randomUUID(), command.message(), now), now));
        return completeWithTools(conversation, userId, now, decision.turnId());
    }

    private static String pausedMessage() {
        return "Fercho est\u00e1 tomando un descanso. Int\u00e9ntalo m\u00e1s tarde.";
    }

    private ChatResponse confirm(
            Conversation conversation, UUID userId, ExplicitConfirmation confirmation, Instant now) {
        if (confirmation.token() == null || confirmation.token().isBlank()) {
            throw new InvalidConfirmationException("confirmation token is required");
        }
        PendingSensitiveAction pending = pendingSensitiveActionStore
                .findByToken(confirmation.token())
                .orElseThrow(() -> new InvalidConfirmationException("confirmation token is not valid"));
        if (!pending.userId().equals(userId)) {
            throw new InvalidConfirmationException("confirmation does not belong to the authenticated user");
        }
        String expectedFingerprint =
                pending.type() == SensitiveActionType.CHECKOUT
                        ? ConfirmationFingerprints.checkout(pending.checkoutCommand())
                        : ConfirmationFingerprints.cancelOrder(pending.orderId());
        if (!pending.fingerprint().equals(expectedFingerprint)) {
            throw new InvalidConfirmationException("confirmation does not match the prepared action");
        }
        String resultText;
        if (pending.type() == SensitiveActionType.CHECKOUT) {
            CheckoutResult result = checkoutUseCase.execute(pending.checkoutCommand());
            resultText = FerchoSensitiveActionCopy.checkoutSucceeded(result.orderNumber(), result.status());
        } else {
            OrderResult result = cancelOrderUseCase.execute(new CancelOrderCommand(pending.orderId()));
            resultText = FerchoSensitiveActionCopy.cancellationSucceeded(result.orderNumber(), result.status());
        }
        pendingSensitiveActionStore.delete(pending.token());
        packageSelectionProposalStore.supersedeActiveOffered(userId, now);
        Conversation updated = conversationStore.save(
                conversation.append(Message.assistant(UUID.randomUUID(), resultText, now), now));
        return new ChatResponse(updated.id(), resultText, false, null, null, false, null);
    }

    private ChatResponse applyPackageSelection(
            Conversation conversation,
            UUID userId,
            PackageSelection selection,
            Instant now,
            UUID turnId) {
        if (pendingSensitiveActionStore.findByUserId(userId).isPresent()) {
            throw new InvalidPackageSelectionException("a sensitive confirmation is already pending");
        }
        if (selection.proposalId() == null || selection.packageCount() < 1) {
            throw new InvalidPackageSelectionException("package selection is not valid");
        }
        boolean started = packageSelectionProposalStore.tryBeginConsuming(
                selection.proposalId(),
                userId,
                selection.packageCount(),
                now,
                PackageSelectionDurations.CONSUMING_LEASE);
        if (!started) {
            return replayExistingSelection(conversation, userId, selection, now, turnId);
        }
        try {
            revalidateOfferedProduct(selection.proposalId(), userId, selection.packageCount(), now);
        } catch (RuntimeException exception) {
            packageSelectionProposalStore.tryRestoreOffered(selection.proposalId(), userId, clockPort.currentTime());
            if (exception instanceof InvalidPackageSelectionException invalid) {
                throw invalid;
            }
            throw new InvalidPackageSelectionException(
                    exception.getMessage() == null ? "package selection is not valid" : exception.getMessage());
        }
        PackageSelectionProposal proposal = packageSelectionProposalStore
                .findById(selection.proposalId(), clockPort.currentTime())
                .orElseThrow(() -> new InvalidPackageSelectionException("package selection is not valid"));
        try {
            applyIdempotentCartAddUseCase.execute(new IdempotentAddProductToCartCommand(
                    proposal.proposalId(), proposal.productId(), selection.packageCount()));
        } catch (CartAddOperationConflictException exception) {
            packageSelectionProposalStore.tryRestoreOffered(selection.proposalId(), userId, clockPort.currentTime());
            throw new InvalidPackageSelectionException("package selection does not match the applied operation");
        } catch (RuntimeException exception) {
            packageSelectionProposalStore.tryRestoreOffered(selection.proposalId(), userId, clockPort.currentTime());
            throw new InvalidPackageSelectionException(
                    exception.getMessage() == null ? "package selection is not valid" : exception.getMessage());
        }
        return completePackageSelection(conversation, userId, selection, proposal, now, turnId);
    }

    private ChatResponse replayExistingSelection(
            Conversation conversation,
            UUID userId,
            PackageSelection selection,
            Instant now,
            UUID turnId) {
        Optional<IdempotentCartAddResult> replayed;
        try {
            replayed = applyIdempotentCartAddUseCase.replayIfApplied(
                    selection.proposalId(), selection.packageCount());
        } catch (CartAddOperationConflictException exception) {
            throw new InvalidPackageSelectionException("package selection does not match the applied operation");
        }
        if (replayed.isEmpty()) {
            throw new InvalidPackageSelectionException(rejectReason(selection, userId, now));
        }
        PackageSelectionProposal proposal = packageSelectionProposalStore
                .findById(selection.proposalId(), clockPort.currentTime())
                .filter(found -> userId.equals(found.userId()))
                .orElse(null);
        return completePackageSelection(conversation, userId, selection, proposal, now, turnId);
    }

    private ChatResponse completePackageSelection(
            Conversation conversation,
            UUID userId,
            PackageSelection selection,
            PackageSelectionProposal proposal,
            Instant now,
            UUID turnId) {
        markConsumedAfterAdd(selection.proposalId(), userId);
        String text = proposal == null
                ? PackageSelectionCopy.alreadyInCart(selection.packageCount())
                : PackageSelectionCopy.added(
                        selection.packageCount(),
                        proposal.productName(),
                        proposal.presentationQuantity(),
                        proposal.presentationUnit());
        Conversation updated = conversationStore.save(
                conversation.append(Message.assistant(UUID.randomUUID(), text, now), now));
        ferchoUsageGate.completeAcceptedTurn(turnId, 0, null, null, null, null, now);
        return new ChatResponse(updated.id(), text, false, null, null, false, null);
    }

    private void revalidateOfferedProduct(UUID proposalId, UUID userId, int packageCount, Instant now) {
        PackageSelectionProposal proposal = packageSelectionProposalStore
                .findById(proposalId, now)
                .orElseThrow(() -> new InvalidPackageSelectionException("package selection is not valid"));
        if (!userId.equals(proposal.userId())) {
            throw new InvalidPackageSelectionException("package selection is not valid");
        }
        ProductResult product;
        try {
            product = getProductUseCase.execute(new GetProductCommand(proposal.productId(), CatalogView.PUBLIC));
        } catch (ProductNotFoundException exception) {
            throw new InvalidPackageSelectionException("product is no longer available");
        }
        if (product.status() != ProductStatus.ACTIVE || product.price() == null) {
            throw new InvalidPackageSelectionException("product is no longer sellable");
        }
        if (product.presentation() == null
                || product.presentation().unit() != proposal.presentationUnit()
                || product.presentation().quantity().compareTo(proposal.presentationQuantity()) != 0) {
            throw new InvalidPackageSelectionException("product presentation changed");
        }
        if (product.stock() < packageCount) {
            throw new InvalidPackageSelectionException("stock is insufficient");
        }
    }

    private void markConsumedAfterAdd(UUID proposalId, UUID userId) {
        for (int attempt = 0; attempt < CONSUMED_CAS_RETRIES; attempt++) {
            if (packageSelectionProposalStore.tryMarkConsumed(proposalId, userId, clockPort.currentTime())) {
                return;
            }
        }
        log.warn(
                "FERCHO_PACKAGE_SELECTION consume_cas_failed after cart add proposalId={} — cart add is not repeated",
                proposalId);
    }

    private String rejectReason(PackageSelection selection, UUID userId, Instant now) {
        PackageSelectionProposal proposal = packageSelectionProposalStore
                .findById(selection.proposalId(), now)
                .orElse(null);
        if (proposal == null || !userId.equals(proposal.userId())) {
            return "package selection is not valid";
        }
        if (proposal.status() == PackageSelectionProposalStatus.EXPIRED) {
            return "package selection has expired";
        }
        if (proposal.status() == PackageSelectionProposalStatus.SUPERSEDED) {
            return "package selection was replaced";
        }
        if (proposal.status() == PackageSelectionProposalStatus.CONSUMED) {
            return "package selection was already used";
        }
        if (proposal.status() == PackageSelectionProposalStatus.CONSUMING) {
            return "package selection is already being processed";
        }
        if (!proposal.allows(selection.packageCount())) {
            return "package count is not an offered option";
        }
        return "package selection is not valid";
    }

    private ChatResponse completeWithTools(Conversation conversation, UUID userId, Instant now, UUID turnId) {
        Optional<PendingSensitiveAction> outstanding = pendingSensitiveActionStore.findByUserId(userId);
        if (outstanding.isPresent()) {
            return pendingConfirmationResponse(
                    conversation,
                    outstanding.get(),
                    now,
                    turnId,
                    0,
                    null,
                    null,
                    0,
                    List.of(),
                    System.nanoTime(),
                    false);
        }
        List<LlmToolDefinition> tools = toolRegistry.definitionsFor(ToolAccess.CUSTOMER);
        boolean authenticationRequired = false;
        String confirmationToken = null;
        Conversation current = conversation;
        List<String> executedTools = new ArrayList<>();
        int roundsUsed = 0;
        int llmCalls = 0;
        Integer promptTokens = null;
        Integer completionTokens = null;
        long totalStartNanos = System.nanoTime();
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            long llmStartNanos = System.nanoTime();
            LlmResponse response;
            try {
                response = llmPort.complete(new LlmRequest(toLlmMessages(current), tools, FerchoSystemPrompt.TEXT));
            } catch (RuntimeException exception) {
                log.info("FERCHO_LLM round={} duration_ms={}", round + 1,
                        (System.nanoTime() - llmStartNanos) / 1_000_000);
                throw exception;
            }
            llmCalls++;
            log.info("FERCHO_LLM round={} duration_ms={}", round + 1, (System.nanoTime() - llmStartNanos) / 1_000_000);
            if (response.usage() != null) {
                promptTokens = (promptTokens == null ? 0 : promptTokens)
                        + (response.usage().promptTokens() == null ? 0 : response.usage().promptTokens());
                completionTokens = (completionTokens == null ? 0 : completionTokens)
                        + (response.usage().completionTokens() == null ? 0 : response.usage().completionTokens());
            }
            if (!response.hasToolCalls()) {
                String text = response.text().isBlank() ? "I could not produce a response." : response.text();
                current = conversationStore.save(
                        current.append(Message.assistant(UUID.randomUUID(), text, now), now));
                roundsUsed = round + 1;
                ferchoUsageGate.completeAcceptedTurn(
                        turnId, llmCalls, promptTokens, completionTokens, null, null, now);
                log.info("FERCHO_TRACE rounds={} tools={} total_duration_ms={}", roundsUsed,
                        String.join(",", executedTools), (System.nanoTime() - totalStartNanos) / 1_000_000);
                return chatResponse(
                        current.id(),
                        text,
                        false,
                        null,
                        null,
                        authenticationRequired,
                        userId,
                        now,
                        executedTools);
            }
            List<MessageToolCall> domainCalls = new ArrayList<>();
            List<Message> toolMessages = new ArrayList<>();
            for (LlmMessage.LlmToolCall call : response.toolCalls()) {
                String callId = call.id() == null || call.id().isBlank() ? UUID.randomUUID().toString() : call.id();
                domainCalls.add(new MessageToolCall(callId, call.name(), call.arguments()));
                ToolResult result;
                String status;
                long toolStartNanos = System.nanoTime();
                try {
                    result = toolRegistry.execute(ToolAccess.CUSTOMER, call.name(), call.arguments());
                    status = statusOf(result);
                } catch (ToolAccessDeniedException exception) {
                    authenticationRequired = true;
                    status = "ACCESS_DENIED";
                    result = ToolResult.failure(
                            "authentication required: this action needs an authenticated customer account");
                } catch (RuntimeException exception) {
                    status = "FAILURE";
                    result = ToolResult.failure(
                            exception.getMessage() == null
                                    ? exception.getClass().getSimpleName()
                                    : exception.getMessage());
                }
                log.info("FERCHO_TOOL round={} tool={} status={} duration_ms={}", round + 1, call.name(), status,
                        (System.nanoTime() - toolStartNanos) / 1_000_000);
                if (result.awaitsConfirmation()) {
                    confirmationToken = result.confirmationToken();
                }
                executedTools.add(call.name());
                toolMessages.add(Message.toolResult(
                        UUID.randomUUID(), callId, call.name(), result.content(), now));
            }
            current = current.append(Message.assistantToolCalls(UUID.randomUUID(), domainCalls, now), now);
            for (Message toolMessage : toolMessages) {
                current = current.append(toolMessage, now);
            }
            current = conversationStore.save(current);
            if (confirmationToken != null) {
                PendingSensitiveAction prepared = pendingSensitiveActionStore
                        .findByToken(confirmationToken)
                        .orElse(null);
                if (prepared != null) {
                    return pendingConfirmationResponse(
                            current,
                            prepared,
                            now,
                            turnId,
                            llmCalls,
                            promptTokens,
                            completionTokens,
                            round + 1,
                            executedTools,
                            totalStartNanos,
                            authenticationRequired);
                }
            }
        }
        String fallback = "Too many tool calls were requested in this turn.";
        current = conversationStore.save(current.append(Message.assistant(UUID.randomUUID(), fallback, now), now));
        ferchoUsageGate.completeAcceptedTurn(turnId, llmCalls, promptTokens, completionTokens, null, null, now);
        log.info("FERCHO_TRACE rounds={} tools={} total_duration_ms={}", MAX_TOOL_ROUNDS,
                String.join(",", executedTools), (System.nanoTime() - totalStartNanos) / 1_000_000);
        return chatResponse(
                current.id(),
                fallback,
                false,
                null,
                null,
                authenticationRequired,
                userId,
                now,
                executedTools);
    }

    private ChatResponse pendingConfirmationResponse(
            Conversation conversation,
            PendingSensitiveAction pending,
            Instant now,
            UUID turnId,
            int llmCalls,
            Integer promptTokens,
            Integer completionTokens,
            int roundsUsed,
            List<String> executedTools,
            long totalStartNanos,
            boolean authenticationRequired) {
        String text = FerchoSensitiveActionCopy.pending(pending.type());
        Conversation updated = conversationStore.save(
                conversation.append(Message.assistant(UUID.randomUUID(), text, now), now));
        ferchoUsageGate.completeAcceptedTurn(turnId, llmCalls, promptTokens, completionTokens, null, null, now);
        log.info("FERCHO_TRACE rounds={} tools={} total_duration_ms={}", roundsUsed,
                String.join(",", executedTools), (System.nanoTime() - totalStartNanos) / 1_000_000);
        return new ChatResponse(
                updated.id(), text, true, pending.token(), pending.type(), authenticationRequired, null);
    }

    private ChatResponse chatResponse(
            UUID conversationId,
            String text,
            boolean awaitingConfirmation,
            String confirmationToken,
            SensitiveActionType confirmationType,
            boolean authenticationRequired,
            UUID userId,
            Instant now,
            List<String> executedTools) {
        if (!awaitingConfirmation && PackageProposalRelevance.shouldDetachActiveOffer(executedTools)) {
            packageSelectionProposalStore.supersedeActiveOffered(userId, now);
        }
        PackageProposalView proposal = awaitingConfirmation
                ? null
                : packageSelectionProposalStore
                        .findActiveOffered(userId, now)
                        .map(PackageProposalViews::from)
                        .orElse(null);
        return new ChatResponse(
                conversationId,
                text,
                awaitingConfirmation,
                confirmationToken,
                confirmationType,
                authenticationRequired,
                proposal);
    }

    private static String statusOf(ToolResult result) {
        if (result.awaitsConfirmation()) {
            return "CONFIRMATION_REQUIRED";
        }
        return result.success() ? "SUCCESS" : "FAILURE";
    }

    private Conversation loadOrStart(UUID conversationId, UUID userId, Instant now) {
        if (conversationId == null) {
            return conversationStore.save(Conversation.start(UUID.randomUUID(), userId, now));
        }
        Conversation conversation = conversationStore
                .findById(conversationId)
                .orElseThrow(ConversationNotFoundException::new);
        if (!conversation.isAccessibleByCustomer(userId)) {
            throw new ConversationNotFoundException();
        }
        return conversation;
    }

    private static List<LlmMessage> toLlmMessages(Conversation conversation) {
        List<LlmMessage> messages = new ArrayList<>();
        for (Message message : conversation.messages()) {
            if (message.role() == MessageRole.USER) {
                messages.add(LlmMessage.user(message.content()));
            } else if (message.role() == MessageRole.TOOL) {
                messages.add(LlmMessage.tool(message.toolCallId(), message.toolName(), message.content()));
            } else if (!message.toolCalls().isEmpty()) {
                messages.add(LlmMessage.assistantToolCalls(message.toolCalls().stream()
                        .map(call -> new LlmMessage.LlmToolCall(call.id(), call.name(), call.arguments()))
                        .toList()));
            } else {
                messages.add(LlmMessage.assistant(message.content()));
            }
        }
        return messages;
    }
}
