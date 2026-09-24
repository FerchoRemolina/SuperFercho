package com.superfercho.assistant.infrastructure.conversation;

import com.superfercho.assistant.application.port.out.ConversationStore;
import com.superfercho.assistant.domain.model.Conversation;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryConversationStore implements ConversationStore {

    private final Map<UUID, Conversation> conversations = new ConcurrentHashMap<>();

    @Override
    public Conversation save(Conversation conversation) {
        conversations.put(conversation.id(), conversation);
        return conversation;
    }

    @Override
    public Optional<Conversation> findById(UUID conversationId) {
        return Optional.ofNullable(conversations.get(conversationId));
    }

    @Override
    public void deleteByUserId(UUID userId) {
        conversations.values().removeIf(conversation -> conversation.userId().equals(userId));
    }
}
