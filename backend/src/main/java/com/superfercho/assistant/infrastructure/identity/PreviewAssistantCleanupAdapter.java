package com.superfercho.assistant.infrastructure.identity;

import com.superfercho.assistant.application.port.out.ConversationStore;
import com.superfercho.assistant.application.port.out.PackageSelectionProposalStore;
import com.superfercho.assistant.application.port.out.PendingSensitiveActionStore;
import com.superfercho.identity.application.port.CustomerAccountAssistantCleanupPort;
import com.superfercho.identity.application.port.PreviewAssistantCleanupPort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PreviewAssistantCleanupAdapter
        implements PreviewAssistantCleanupPort, CustomerAccountAssistantCleanupPort {

    private final ConversationStore conversationStore;
    private final PendingSensitiveActionStore pendingSensitiveActionStore;
    private final PackageSelectionProposalStore packageSelectionProposalStore;

    public PreviewAssistantCleanupAdapter(
            ConversationStore conversationStore,
            PendingSensitiveActionStore pendingSensitiveActionStore,
            PackageSelectionProposalStore packageSelectionProposalStore) {
        this.conversationStore = conversationStore;
        this.pendingSensitiveActionStore = pendingSensitiveActionStore;
        this.packageSelectionProposalStore = packageSelectionProposalStore;
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        conversationStore.deleteByUserId(userId);
        pendingSensitiveActionStore.deleteByUserId(userId);
        packageSelectionProposalStore.deleteByUserId(userId);
    }
}
