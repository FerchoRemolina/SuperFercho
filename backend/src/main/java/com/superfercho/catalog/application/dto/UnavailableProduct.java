package com.superfercho.catalog.application.dto;

import java.util.UUID;

public record UnavailableProduct(UUID productId, int requestedQuantity) {
}
