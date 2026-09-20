package com.superfercho.assistant.domain.model;

import com.superfercho.assistant.domain.exception.InvalidMessageException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Message {

    private final UUID id;
    private final MessageRole role;
    private final String content;
    private final Instant occurredAt;
    private final List<MessageToolCall> toolCalls;
    private final String toolCallId;
    private final String toolName;

    private Message(
            UUID id,
            MessageRole role,
            String content,
            Instant occurredAt,
            List<MessageToolCall> toolCalls,
            String toolCallId,
            String toolName) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.occurredAt = occurredAt;
        this.toolCalls = toolCalls;
        this.toolCallId = toolCallId;
        this.toolName = toolName;
    }

    public static Message user(UUID id, String content, Instant occurredAt) {
        return of(id, MessageRole.USER, content, occurredAt, List.of(), null, null);
    }

    public static Message assistant(UUID id, String content, Instant occurredAt) {
        return of(id, MessageRole.ASSISTANT, content, occurredAt, List.of(), null, null);
    }

    public static Message assistantToolCalls(
            UUID id, List<MessageToolCall> toolCalls, Instant occurredAt) {
        return of(id, MessageRole.ASSISTANT, "", occurredAt, toolCalls, null, null);
    }

    public static Message toolResult(
            UUID id, String toolCallId, String toolName, String content, Instant occurredAt) {
        return of(id, MessageRole.TOOL, content, occurredAt, List.of(), toolCallId, toolName);
    }

    public UUID id() {
        return id;
    }

    public MessageRole role() {
        return role;
    }

    public String content() {
        return content;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public List<MessageToolCall> toolCalls() {
        return toolCalls;
    }

    public String toolCallId() {
        return toolCallId;
    }

    public String toolName() {
        return toolName;
    }

    private static Message of(
            UUID id,
            MessageRole role,
            String content,
            Instant occurredAt,
            List<MessageToolCall> toolCalls,
            String toolCallId,
            String toolName) {
        if (id == null) {
            throw new InvalidMessageException("id cannot be null");
        }
        if (role == null) {
            throw new InvalidMessageException("role cannot be null");
        }
        if (occurredAt == null) {
            throw new InvalidMessageException("occurredAt cannot be null");
        }
        if (content == null) {
            throw new InvalidMessageException("content cannot be null");
        }
        List<MessageToolCall> calls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        if (role == MessageRole.USER && content.isBlank()) {
            throw new InvalidMessageException("user content cannot be blank");
        }
        if (role == MessageRole.ASSISTANT && content.isBlank() && calls.isEmpty()) {
            throw new InvalidMessageException("assistant message must have content or tool calls");
        }
        if (role == MessageRole.TOOL) {
            if (toolCallId == null || toolCallId.isBlank()) {
                throw new InvalidMessageException("tool result must include toolCallId");
            }
            if (toolName == null || toolName.isBlank()) {
                throw new InvalidMessageException("tool result must include toolName");
            }
        }
        if (role != MessageRole.ASSISTANT && !calls.isEmpty()) {
            throw new InvalidMessageException("only assistant messages can include tool calls");
        }
        if (role != MessageRole.TOOL && (toolCallId != null || toolName != null)) {
            throw new InvalidMessageException("toolCallId and toolName are only valid for tool results");
        }
        return new Message(id, role, content, occurredAt, calls, toolCallId, toolName);
    }
}
