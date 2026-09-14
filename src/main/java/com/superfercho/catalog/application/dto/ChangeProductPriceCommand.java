package com.superfercho.catalog.application.dto;

import com.superfercho.platform.money.Money;
import java.util.UUID;

public record ChangeProductPriceCommand(UUID productId, Money price) {
}
