package com.superfercho.assistant.application.service;

import com.superfercho.assistant.application.confirmation.ConfirmationFingerprints;
import com.superfercho.assistant.application.confirmation.PendingSensitiveAction;
import com.superfercho.assistant.application.confirmation.SensitiveActionType;
import com.superfercho.assistant.application.dto.chat.ChatCommand;
import com.superfercho.assistant.application.dto.chat.ChatResponse;
import com.superfercho.assistant.application.dto.chat.ExplicitConfirmation;
import com.superfercho.assistant.application.dto.llm.LlmMessage;
import com.superfercho.assistant.application.dto.llm.LlmRequest;
import com.superfercho.assistant.application.dto.llm.LlmResponse;
import com.superfercho.assistant.application.dto.llm.LlmToolDefinition;
import com.superfercho.assistant.application.exception.ConversationNotFoundException;
import com.superfercho.assistant.application.exception.InvalidChatRequestException;
import com.superfercho.assistant.application.exception.InvalidConfirmationException;
import com.superfercho.assistant.application.port.in.ChatUseCase;
import com.superfercho.assistant.application.port.out.ClockPort;
import com.superfercho.assistant.application.port.out.ConversationStore;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.port.out.LLMPort;
import com.superfercho.assistant.application.port.out.PendingSensitiveActionStore;
import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolRegistry;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.domain.model.Conversation;
import com.superfercho.assistant.domain.model.Message;
import com.superfercho.assistant.domain.model.MessageRole;
import com.superfercho.assistant.domain.model.MessageToolCall;
import com.superfercho.orders.application.dto.CancelOrderCommand;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.port.in.CancelOrderUseCase;
import com.superfercho.orders.application.port.in.CheckoutUseCase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ChatApplicationService implements ChatUseCase {

    private static final int MAX_TOOL_ROUNDS = 8;

    private final CurrentUserProvider currentUserProvider;
    private final ClockPort clockPort;
    private final ConversationStore conversationStore;
    private final PendingSensitiveActionStore pendingSensitiveActionStore;
    private final LLMPort llmPort;
    private final ToolRegistry toolRegistry;
    private final CheckoutUseCase checkoutUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    public ChatApplicationService(
            CurrentUserProvider currentUserProvider,
            ClockPort clockPort,
            ConversationStore conversationStore,
            PendingSensitiveActionStore pendingSensitiveActionStore,
            LLMPort llmPort,
            ToolRegistry toolRegistry,
            CheckoutUseCase checkoutUseCase,
            CancelOrderUseCase cancelOrderUseCase) {
        this.currentUserProvider = currentUserProvider;
        this.clockPort = clockPort;
        this.conversationStore = conversationStore;
        this.pendingSensitiveActionStore = pendingSensitiveActionStore;
        this.llmPort = llmPort;
        this.toolRegistry = toolRegistry;
        this.checkoutUseCase = checkoutUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
    }

    @Override
    public ChatResponse execute(ChatCommand command) {
        if (command == null) {
            throw new InvalidChatRequestException("command cannot be null");
        }
        UUID userId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        Conversation conversation = loadOrStart(command.conversationId(), userId, now);
        if (command.confirmation() != null) {
            return confirm(conversation, userId, command.confirmation(), now);
        }
        if (command.message() == null || command.message().isBlank()) {
            throw new InvalidChatRequestException("message cannot be blank");
        }
        conversation = conversationStore.save(conversation.append(Message.user(UUID.randomUUID(), command.message(), now), now));
        return completeWithTools(conversation, now);
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
            resultText = "Checkout confirmed: order " + result.orderNumber() + " status " + result.status();
        } else {
            OrderResult result = cancelOrderUseCase.execute(new CancelOrderCommand(pending.orderId()));
            resultText = "Cancellation confirmed: order " + result.orderNumber() + " status " + result.status();
        }
        pendingSensitiveActionStore.delete(pending.token());
        Conversation updated = conversationStore.save(
                conversation.append(Message.assistant(UUID.randomUUID(), resultText, now), now));
        return new ChatResponse(updated.id(), resultText, false, null, null);
    }

    private ChatResponse completeWithTools(Conversation conversation, Instant now) {
        List<LlmToolDefinition> tools =
                toolRegistry.allowlist().stream().map(AssistantTool::definition).toList();
        String confirmationToken = null;
        SensitiveActionType confirmationType = null;
        Conversation current = conversation;
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            LlmResponse response = llmPort.complete(new LlmRequest(toLlmMessages(current), tools));
            if (!response.hasToolCalls()) {
                String text = response.text().isBlank() ? "I could not produce a response." : response.text();
                current = conversationStore.save(
                        current.append(Message.assistant(UUID.randomUUID(), text, now), now));
                return new ChatResponse(
                        current.id(), text, confirmationToken != null, confirmationToken, confirmationType);
            }
            List<MessageToolCall> domainCalls = new ArrayList<>();
            List<Message> toolMessages = new ArrayList<>();
            for (LlmMessage.LlmToolCall call : response.toolCalls()) {
                String callId = call.id() == null || call.id().isBlank() ? UUID.randomUUID().toString() : call.id();
                domainCalls.add(new MessageToolCall(callId, call.name(), call.arguments()));
                ToolResult result;
                try {
                    result = toolRegistry.execute(call.name(), call.arguments());
                } catch (RuntimeException exception) {
                    result = ToolResult.failure(
                            exception.getMessage() == null
                                    ? exception.getClass().getSimpleName()
                                    : exception.getMessage());
                }
                if (result.awaitsConfirmation()) {
                    confirmationToken = result.confirmationToken();
                    confirmationType = confirmationTypeOf(call.name());
                }
                toolMessages.add(Message.toolResult(
                        UUID.randomUUID(), callId, call.name(), result.content(), now));
            }
            current = current.append(Message.assistantToolCalls(UUID.randomUUID(), domainCalls, now), now);
            for (Message toolMessage : toolMessages) {
                current = current.append(toolMessage, now);
            }
            current = conversationStore.save(current);
        }
        String fallback = "Too many tool calls were requested in this turn.";
        current = conversationStore.save(current.append(Message.assistant(UUID.randomUUID(), fallback, now), now));
        return new ChatResponse(current.id(), fallback, confirmationToken != null, confirmationToken, confirmationType);
    }

    private Conversation loadOrStart(UUID conversationId, UUID userId, Instant now) {
        if (conversationId == null) {
            return conversationStore.save(Conversation.start(UUID.randomUUID(), userId, now));
        }
        Conversation conversation = conversationStore
                .findById(conversationId)
                .orElseThrow(ConversationNotFoundException::new);
        if (!conversation.userId().equals(userId)) {
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

    private static SensitiveActionType confirmationTypeOf(String toolName) {
        if (com.superfercho.assistant.application.tool.ToolNames.CHECKOUT.equals(toolName)) {
            return SensitiveActionType.CHECKOUT;
        }
        if (com.superfercho.assistant.application.tool.ToolNames.CANCEL_ORDER.equals(toolName)) {
            return SensitiveActionType.CANCEL_ORDER;
        }
        return null;
    }
}
