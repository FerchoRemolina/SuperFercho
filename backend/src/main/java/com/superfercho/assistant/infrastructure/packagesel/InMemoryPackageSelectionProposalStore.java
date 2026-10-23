package com.superfercho.assistant.infrastructure.packagesel;

import com.superfercho.assistant.application.packagesel.PackageSelectionProposal;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposalStatus;
import com.superfercho.assistant.application.port.out.PackageSelectionProposalStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory proposals are not in the same transaction as the PostgreSQL cart.
 * If the process dies after a successful cart add and before {@code CONSUMED}, the
 * line remains in the cart and the proposal may return to {@code OFFERED} when the
 * {@code CONSUMING} lease expires. Recovery retries only the consume CAS; it never
 * repeats the cart add automatically.
 */
public final class InMemoryPackageSelectionProposalStore implements PackageSelectionProposalStore {

    private final ConcurrentHashMap<UUID, PackageSelectionProposal> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> activeIdByUser = new ConcurrentHashMap<>();

    @Override
    public Optional<PackageSelectionProposal> replaceOffered(PackageSelectionProposal proposal, Instant now) {
        UUID userId = proposal.userId();
        AtomicReference<PackageSelectionProposal> stored = new AtomicReference<>();
        activeIdByUser.compute(userId, (id, currentActiveId) -> {
            if (currentActiveId != null) {
                AtomicBoolean blocked = new AtomicBoolean(false);
                byId.computeIfPresent(currentActiveId, (proposalId, current) -> {
                    PackageSelectionProposal refreshed = refresh(current, now);
                    if (isLiveConsuming(refreshed, now)) {
                        blocked.set(true);
                        return refreshed;
                    }
                    if (refreshed.status() == PackageSelectionProposalStatus.OFFERED) {
                        return refreshed.withStatus(PackageSelectionProposalStatus.SUPERSEDED, null);
                    }
                    return refreshed;
                });
                if (blocked.get()) {
                    return currentActiveId;
                }
            }
            byId.put(proposal.proposalId(), proposal);
            stored.set(proposal);
            return proposal.proposalId();
        });
        return Optional.ofNullable(stored.get());
    }

    @Override
    public Optional<PackageSelectionProposal> findById(UUID proposalId, Instant now) {
        if (proposalId == null) {
            return Optional.empty();
        }
        AtomicReference<PackageSelectionProposal> found = new AtomicReference<>();
        byId.computeIfPresent(proposalId, (id, current) -> {
            PackageSelectionProposal refreshed = refresh(current, now);
            found.set(refreshed);
            return refreshed;
        });
        return Optional.ofNullable(found.get());
    }

    @Override
    public Optional<PackageSelectionProposal> findActiveOffered(UUID userId, Instant now) {
        if (userId == null) {
            return Optional.empty();
        }
        UUID activeId = activeIdByUser.get(userId);
        if (activeId == null) {
            return Optional.empty();
        }
        return findById(activeId, now).filter(proposal -> proposal.status() == PackageSelectionProposalStatus.OFFERED);
    }

    @Override
    public Optional<PackageSelectionProposal> findBlocking(UUID userId, UUID productId, Instant now) {
        if (userId == null || productId == null) {
            return Optional.empty();
        }
        UUID activeId = activeIdByUser.get(userId);
        if (activeId == null) {
            return Optional.empty();
        }
        return findById(activeId, now)
                .filter(proposal -> productId.equals(proposal.productId()))
                .filter(proposal -> blocksAdd(proposal, now));
    }

    @Override
    public boolean tryBeginConsuming(
            UUID proposalId, UUID userId, int packageCount, Instant now, Duration lease) {
        AtomicBoolean started = new AtomicBoolean(false);
        byId.computeIfPresent(proposalId, (id, current) -> {
            PackageSelectionProposal refreshed = refresh(current, now);
            if (!userId.equals(refreshed.userId())
                    || refreshed.status() != PackageSelectionProposalStatus.OFFERED
                    || !refreshed.allows(packageCount)
                    || !now.isBefore(refreshed.expiresAt())) {
                return refreshed;
            }
            started.set(true);
            return refreshed.withStatus(PackageSelectionProposalStatus.CONSUMING, now.plus(lease));
        });
        return started.get();
    }

    @Override
    public boolean tryRestoreOffered(UUID proposalId, UUID userId, Instant now) {
        AtomicBoolean restored = new AtomicBoolean(false);
        byId.computeIfPresent(proposalId, (id, current) -> {
            PackageSelectionProposal refreshed = refresh(current, now);
            if (!userId.equals(refreshed.userId())
                    || refreshed.status() != PackageSelectionProposalStatus.CONSUMING) {
                return refreshed;
            }
            if (!now.isBefore(refreshed.expiresAt())) {
                return refreshed.withStatus(PackageSelectionProposalStatus.EXPIRED, null);
            }
            restored.set(true);
            return refreshed.withStatus(PackageSelectionProposalStatus.OFFERED, null);
        });
        return restored.get();
    }

    @Override
    public boolean tryMarkConsumed(UUID proposalId, UUID userId, Instant now) {
        AtomicBoolean consumed = new AtomicBoolean(false);
        byId.computeIfPresent(proposalId, (id, current) -> {
            PackageSelectionProposal refreshed = refresh(current, now);
            if (!userId.equals(refreshed.userId())
                    || refreshed.status() != PackageSelectionProposalStatus.CONSUMING) {
                return refreshed;
            }
            consumed.set(true);
            return refreshed.withStatus(PackageSelectionProposalStatus.CONSUMED, null);
        });
        if (consumed.get()) {
            activeIdByUser.remove(userId, proposalId);
        }
        return consumed.get();
    }

    @Override
    public void deleteByUserId(UUID userId) {
        if (userId == null) {
            return;
        }
        UUID activeId = activeIdByUser.remove(userId);
        if (activeId != null) {
            byId.remove(activeId);
        }
        byId.values().removeIf(proposal -> userId.equals(proposal.userId()));
    }

    private static boolean blocksAdd(PackageSelectionProposal proposal, Instant now) {
        return proposal.status() == PackageSelectionProposalStatus.OFFERED
                || isLiveConsuming(proposal, now);
    }

    private static boolean isLiveConsuming(PackageSelectionProposal proposal, Instant now) {
        return proposal.status() == PackageSelectionProposalStatus.CONSUMING
                && proposal.consumingUntil() != null
                && now.isBefore(proposal.consumingUntil());
    }

    private static PackageSelectionProposal refresh(PackageSelectionProposal current, Instant now) {
        if (current.status() == PackageSelectionProposalStatus.CONSUMING
                && (current.consumingUntil() == null || !now.isBefore(current.consumingUntil()))) {
            if (now.isBefore(current.expiresAt())) {
                return current.withStatus(PackageSelectionProposalStatus.OFFERED, null);
            }
            return current.withStatus(PackageSelectionProposalStatus.EXPIRED, null);
        }
        if (current.status() == PackageSelectionProposalStatus.OFFERED && !now.isBefore(current.expiresAt())) {
            return current.withStatus(PackageSelectionProposalStatus.EXPIRED, null);
        }
        return current;
    }
}
