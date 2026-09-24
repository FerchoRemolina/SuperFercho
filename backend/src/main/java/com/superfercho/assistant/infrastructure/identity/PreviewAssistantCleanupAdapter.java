package com.superfercho.assistant.infrastructure.identity;

import com.superfercho.assistant.application.port.out.ConversationStore;
import com.superfercho.assistant.application.port.out.PendingSensitiveActionStore;
import com.superfercho.identity.application.port.PreviewAssistantCleanupPort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PreviewAssistantCleanupAdapter implements PreviewAssistantCleanupPort {

    private final ConversationStore conversationStore;
    private final PendingSensitiveActionStore pendingSensitiveActionStore;

    public PreviewAssistantCleanupAdapter(
            ConversationStore conversationStore, PendingSensitiveActionStore pendingSensitiveActionStore) {
        this.conversationStore = conversationStore;
        this.pendingSensitiveActionStore = pendingSensitiveActionStore;
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        conversationStore.deleteByUserId(userId);
        pendingSensitiveActionStore.deleteByUserId(userId);
    }
}
