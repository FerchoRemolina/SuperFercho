package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.PresentationUnit;
import java.math.BigDecimal;
import java.util.UUID;

public record QuantityEquivalenceCommand(UUID productId, BigDecimal quantity, PresentationUnit unit) {}
