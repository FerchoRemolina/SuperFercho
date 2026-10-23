package com.superfercho.assistant.application.dto.chat;

import com.superfercho.catalog.domain.model.PresentationUnit;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PackageProposalView(
        UUID proposalId,
        UUID productId,
        String productName,
        BigDecimal presentationQuantity,
        PresentationUnit presentationUnit,
        BigDecimal requestedQuantity,
        PresentationUnit requestedUnit,
        List<PackageProposalOptionView> options) {}
