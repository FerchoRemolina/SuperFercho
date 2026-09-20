package com.superfercho.assistant.domain.model;

import com.superfercho.assistant.domain.exception.InvalidConversationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Conversation {

    private final UUID id;
    private final UUID userId;
    private final List<Message> messages;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Conversation(UUID id, UUID userId, List<Message> messages, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.messages = messages;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Conversation start(UUID id, UUID userId, Instant createdAt) {
        return of(id, userId, List.of(), createdAt, createdAt);
    }

    public static Conversation reconstitute(
            UUID id, UUID userId, List<Message> messages, Instant createdAt, Instant updatedAt) {
        return of(id, userId, messages, createdAt, updatedAt);
    }

    public Conversation append(Message message, Instant currentTime) {
        if (message == null) {
            throw new InvalidConversationException("message cannot be null");
        }
        if (currentTime == null) {
            throw new InvalidConversationException("currentTime cannot be null");
        }
        if (currentTime.isBefore(createdAt)) {
            throw new InvalidConversationException("currentTime must not be before createdAt");
        }
        List<Message> next = new ArrayList<>(messages);
        next.add(message);
        return of(id, userId, next, createdAt, currentTime);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public List<Message> messages() {
        return messages;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static Conversation of(
            UUID id, UUID userId, List<Message> messages, Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new InvalidConversationException("id cannot be null");
        }
        if (userId == null) {
            throw new InvalidConversationException("userId cannot be null");
        }
        if (createdAt == null) {
            throw new InvalidConversationException("createdAt cannot be null");
        }
        if (updatedAt == null) {
            throw new InvalidConversationException("updatedAt cannot be null");
        }
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidConversationException("createdAt must not be after updatedAt");
        }
        if (messages == null) {
            throw new InvalidConversationException("messages cannot be null");
        }
        for (Message message : messages) {
            if (message == null) {
                throw new InvalidConversationException("messages cannot contain null");
            }
        }
        return new Conversation(id, userId, List.copyOf(messages), createdAt, updatedAt);
    }
}
