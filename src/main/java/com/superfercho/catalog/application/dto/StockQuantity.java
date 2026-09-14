package com.superfercho.catalog.application.dto;

import java.util.UUID;

public record StockQuantity(UUID productId, int quantity) {
}
