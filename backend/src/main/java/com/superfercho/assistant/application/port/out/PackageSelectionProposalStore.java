package com.superfercho.assistant.application.port.out;

import com.superfercho.assistant.application.packagesel.PackageSelectionProposal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PackageSelectionProposalStore {

    /**
     * Replaces a previous {@code OFFERED} proposal for the user. Leaves a live {@code CONSUMING}
     * lease untouched and does not store the incoming offer in that case.
     *
     * @return the stored offer, or empty when a live consuming lease blocked replacement
     */
    Optional<PackageSelectionProposal> replaceOffered(PackageSelectionProposal proposal, Instant now);

    Optional<PackageSelectionProposal> findById(UUID proposalId, Instant now);

    Optional<PackageSelectionProposal> findBlocking(UUID userId, UUID productId, Instant now);

    Optional<PackageSelectionProposal> findActiveOffered(UUID userId, Instant now);

    boolean tryBeginConsuming(
            UUID proposalId, UUID userId, int packageCount, Instant now, Duration lease);

    boolean tryRestoreOffered(UUID proposalId, UUID userId, Instant now);

    boolean tryMarkConsumed(UUID proposalId, UUID userId, Instant now);

    void deleteByUserId(UUID userId);
}
