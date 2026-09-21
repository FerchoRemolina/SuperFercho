package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.money.Money;
import java.util.UUID;

public record ProductCardInfo(
        UUID id,
        String name,
        String brand,
        Money price,
        String imageUrl,
        UUID categoryId,
        ProductStatus status,
        boolean sellable) {}
