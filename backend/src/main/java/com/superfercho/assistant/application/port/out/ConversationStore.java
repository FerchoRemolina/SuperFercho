package com.superfercho.assistant.application.port.out;

import com.superfercho.assistant.domain.model.Conversation;
import java.util.Optional;
import java.util.UUID;

public interface ConversationStore {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(UUID conversationId);

    void deleteByUserId(UUID userId);
}
