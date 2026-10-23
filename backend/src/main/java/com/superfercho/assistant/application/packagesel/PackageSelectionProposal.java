package com.superfercho.assistant.application.packagesel;

import com.superfercho.catalog.domain.model.PresentationUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record PackageSelectionProposal(
        UUID proposalId,
        UUID userId,
        UUID productId,
        String productName,
        BigDecimal presentationQuantity,
        PresentationUnit presentationUnit,
        List<PackageSelectionOption> options,
        BigDecimal requestedQuantity,
        PresentationUnit requestedUnit,
        PackageSelectionProposalStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant consumingUntil) {

    public PackageSelectionProposal {
        Objects.requireNonNull(proposalId, "proposalId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(productName, "productName");
        Objects.requireNonNull(presentationQuantity, "presentationQuantity");
        Objects.requireNonNull(presentationUnit, "presentationUnit");
        Objects.requireNonNull(options, "options");
        if (options.isEmpty()) {
            throw new IllegalArgumentException("options cannot be empty");
        }
        options = List.copyOf(options);
        Objects.requireNonNull(requestedQuantity, "requestedQuantity");
        Objects.requireNonNull(requestedUnit, "requestedUnit");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public Set<Integer> allowedPackageCounts() {
        return options.stream().map(PackageSelectionOption::packageCount).collect(Collectors.toUnmodifiableSet());
    }

    public boolean allows(int packageCount) {
        return allowedPackageCounts().contains(packageCount);
    }

    public PackageSelectionProposal withStatus(
            PackageSelectionProposalStatus nextStatus, Instant nextConsumingUntil) {
        return new PackageSelectionProposal(
                proposalId,
                userId,
                productId,
                productName,
                presentationQuantity,
                presentationUnit,
                options,
                requestedQuantity,
                requestedUnit,
                nextStatus,
                createdAt,
                expiresAt,
                nextConsumingUntil);
    }
}
