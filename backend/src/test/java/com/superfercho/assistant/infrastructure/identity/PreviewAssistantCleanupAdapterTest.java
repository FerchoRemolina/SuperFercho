package com.superfercho.assistant.infrastructure.identity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.assistant.application.packagesel.PackageSelectionDurations;
import com.superfercho.assistant.application.packagesel.PackageSelectionOption;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposal;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposalStatus;
import com.superfercho.assistant.infrastructure.confirmation.InMemoryPendingSensitiveActionStore;
import com.superfercho.assistant.infrastructure.conversation.InMemoryConversationStore;
import com.superfercho.assistant.infrastructure.packagesel.InMemoryPackageSelectionProposalStore;
import com.superfercho.catalog.domain.model.PresentationUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PreviewAssistantCleanupAdapterTest {

    @Test
    void deleteAllForUserRemovesPackageProposals() {
        Instant now = Instant.parse("2026-10-23T18:00:00Z");
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID otherUser = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID productId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        InMemoryConversationStore conversations = new InMemoryConversationStore();
        InMemoryPendingSensitiveActionStore pending = new InMemoryPendingSensitiveActionStore();
        InMemoryPackageSelectionProposalStore proposals = new InMemoryPackageSelectionProposalStore();
        proposals.replaceOffered(
                new PackageSelectionProposal(
                        UUID.randomUUID(),
                        userId,
                        productId,
                        "Arroz",
                        new BigDecimal("1.000"),
                        PresentationUnit.KG,
                        List.of(new PackageSelectionOption(2, new BigDecimal("2000.000"), PresentationUnit.G)),
                        new BigDecimal("2.000"),
                        PresentationUnit.KG,
                        PackageSelectionProposalStatus.OFFERED,
                        now,
                        now.plus(PackageSelectionDurations.PROPOSAL_TTL),
                        null),
                now);
        proposals.replaceOffered(
                new PackageSelectionProposal(
                        UUID.randomUUID(),
                        otherUser,
                        productId,
                        "Arroz",
                        new BigDecimal("1.000"),
                        PresentationUnit.KG,
                        List.of(new PackageSelectionOption(2, new BigDecimal("2000.000"), PresentationUnit.G)),
                        new BigDecimal("2.000"),
                        PresentationUnit.KG,
                        PackageSelectionProposalStatus.OFFERED,
                        now,
                        now.plus(PackageSelectionDurations.PROPOSAL_TTL),
                        null),
                now);

        new PreviewAssistantCleanupAdapter(conversations, pending, proposals).deleteAllForUser(userId);

        assertTrue(proposals.findActiveOffered(userId, now).isEmpty());
        assertTrue(proposals.findActiveOffered(otherUser, now).isPresent());
    }
}
