package com.superfercho.shopping.application.dto.cart;

import java.util.UUID;

public record IdempotentAddProductToCartCommand(UUID operationId, UUID productId, int quantity) {}
