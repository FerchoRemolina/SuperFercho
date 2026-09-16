package com.superfercho.catalog.infrastructure.rest.dto;

import com.superfercho.platform.money.Money;

public record ChangeProductPriceRequest(Money price) {
}
