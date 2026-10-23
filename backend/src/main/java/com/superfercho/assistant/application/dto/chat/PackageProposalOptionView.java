package com.superfercho.assistant.application.dto.chat;

import com.superfercho.catalog.domain.model.PresentationUnit;
import java.math.BigDecimal;

public record PackageProposalOptionView(
        int packageCount, BigDecimal physicalTotalBase, PresentationUnit baseUnit) {}
