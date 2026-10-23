package com.superfercho.assistant.infrastructure.packagesel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.assistant.application.packagesel.PackageSelectionDurations;
import com.superfercho.assistant.application.packagesel.PackageSelectionOption;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposal;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposalStatus;
import com.superfercho.catalog.domain.model.PresentationUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryPackageSelectionProposalStoreTest {

    private static final Instant T0 = Instant.parse("2026-10-23T18:00:00Z");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private final InMemoryPackageSelectionProposalStore store = new InMemoryPackageSelectionProposalStore();

    @Test
    void replaceOfferedSupersedesPreviousOffer() {
        PackageSelectionProposal first = proposal(UUID.randomUUID(), USER_ID, PRODUCT_ID, T0, 2);
        PackageSelectionProposal second = proposal(UUID.randomUUID(), USER_ID, PRODUCT_ID, T0, 3);
        store.replaceOffered(first, T0);
        store.replaceOffered(second, T0);

        assertEquals(PackageSelectionProposalStatus.SUPERSEDED, store.findById(first.proposalId(), T0).orElseThrow().status());
        assertEquals(PackageSelectionProposalStatus.OFFERED, store.findById(second.proposalId(), T0).orElseThrow().status());
        assertEquals(second.proposalId(), store.findActiveOffered(USER_ID, T0).orElseThrow().proposalId());
    }

    @Test
    void liveConsumingLeaseBlocksReplacement() {
        PackageSelectionProposal first = proposal(UUID.randomUUID(), USER_ID, PRODUCT_ID, T0, 2);
        store.replaceOffered(first, T0);
        assertTrue(store.tryBeginConsuming(
                first.proposalId(), USER_ID, 2, T0, PackageSelectionDurations.CONSUMING_LEASE));

        PackageSelectionProposal second = proposal(UUID.randomUUID(), USER_ID, PRODUCT_ID, T0, 3);
        assertTrue(store.replaceOffered(second, T0).isEmpty());
        assertEquals(PackageSelectionProposalStatus.CONSUMING, store.findById(first.proposalId(), T0).orElseThrow().status());
        assertTrue(store.findById(second.proposalId(), T0).isEmpty());
    }

    @Test
    void expiredConsumingLeaseIsRecoveredToOffered() {
        PackageSelectionProposal offered = proposal(UUID.randomUUID(), USER_ID, PRODUCT_ID, T0, 2);
        store.replaceOffered(offered, T0);
        assertTrue(store.tryBeginConsuming(offered.proposalId(), USER_ID, 2, T0, PackageSelectionDurations.CONSUMING_LEASE));

        Instant afterLease = T0.plus(PackageSelectionDurations.CONSUMING_LEASE).plusSeconds(1);
        assertEquals(
                PackageSelectionProposalStatus.OFFERED,
                store.findById(offered.proposalId(), afterLease).orElseThrow().status());
        assertTrue(store.findActiveOffered(USER_ID, afterLease).isPresent());
    }

    @Test
    void proposalExpiresAfterTtl() {
        PackageSelectionProposal offered = proposal(UUID.randomUUID(), USER_ID, PRODUCT_ID, T0, 2);
        store.replaceOffered(offered, T0);
        Instant expiredAt = T0.plus(PackageSelectionDurations.PROPOSAL_TTL);
        assertEquals(
                PackageSelectionProposalStatus.EXPIRED,
                store.findById(offered.proposalId(), expiredAt).orElseThrow().status());
        assertTrue(store.findActiveOffered(USER_ID, expiredAt).isEmpty());
        assertFalse(store.tryBeginConsuming(
                offered.proposalId(), USER_ID, 2, expiredAt, PackageSelectionDurations.CONSUMING_LEASE));
    }

    @Test
    void concurrentBeginConsumingSucceedsAtMostOnce() throws InterruptedException {
        PackageSelectionProposal offered = proposal(UUID.randomUUID(), USER_ID, PRODUCT_ID, T0, 2);
        store.replaceOffered(offered, T0);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger wins = new AtomicInteger();
        Thread first = new Thread(() -> {
            await(start);
            if (store.tryBeginConsuming(
                    offered.proposalId(), USER_ID, 2, T0, PackageSelectionDurations.CONSUMING_LEASE)) {
                wins.incrementAndGet();
            }
        });
        Thread second = new Thread(() -> {
            await(start);
            if (store.tryBeginConsuming(
                    offered.proposalId(), USER_ID, 2, T0, PackageSelectionDurations.CONSUMING_LEASE)) {
                wins.incrementAndGet();
            }
        });
        first.start();
        second.start();
        start.countDown();
        first.join();
        second.join();
        assertEquals(1, wins.get());
    }

    @Test
    void restoreOfferedAfterFailedConsumeAndMarkConsumed() {
        PackageSelectionProposal offered = proposal(UUID.randomUUID(), USER_ID, PRODUCT_ID, T0, 2);
        store.replaceOffered(offered, T0);
        assertTrue(store.tryBeginConsuming(
                offered.proposalId(), USER_ID, 2, T0, PackageSelectionDurations.CONSUMING_LEASE));
        assertTrue(store.tryRestoreOffered(offered.proposalId(), USER_ID, T0));
        assertEquals(PackageSelectionProposalStatus.OFFERED, store.findById(offered.proposalId(), T0).orElseThrow().status());

        assertTrue(store.tryBeginConsuming(
                offered.proposalId(), USER_ID, 2, T0, PackageSelectionDurations.CONSUMING_LEASE));
        assertTrue(store.tryMarkConsumed(offered.proposalId(), USER_ID, T0));
        assertEquals(PackageSelectionProposalStatus.CONSUMED, store.findById(offered.proposalId(), T0).orElseThrow().status());
        assertTrue(store.findActiveOffered(USER_ID, T0).isEmpty());
        assertFalse(store.tryMarkConsumed(offered.proposalId(), USER_ID, T0));
    }

    @Test
    void deleteByUserIdRemovesOnlyThatUsersProposals() {
        PackageSelectionProposal mine = proposal(UUID.randomUUID(), USER_ID, PRODUCT_ID, T0, 2);
        PackageSelectionProposal theirs = proposal(UUID.randomUUID(), OTHER_USER, PRODUCT_ID, T0, 2);
        store.replaceOffered(mine, T0);
        store.replaceOffered(theirs, T0);
        store.deleteByUserId(USER_ID);
        assertTrue(store.findById(mine.proposalId(), T0).isEmpty());
        assertTrue(store.findActiveOffered(OTHER_USER, T0).isPresent());
    }

    static PackageSelectionProposal proposal(UUID id, UUID userId, UUID productId, Instant now, int packageCount) {
        return new PackageSelectionProposal(
                id,
                userId,
                productId,
                "Arroz",
                new BigDecimal("1.000"),
                PresentationUnit.KG,
                List.of(new PackageSelectionOption(packageCount, new BigDecimal("1000.000"), PresentationUnit.G)),
                new BigDecimal("1.000"),
                PresentationUnit.KG,
                PackageSelectionProposalStatus.OFFERED,
                now,
                now.plus(PackageSelectionDurations.PROPOSAL_TTL),
                null);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
