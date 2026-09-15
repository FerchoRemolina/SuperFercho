package com.superfercho.shopping.application.dto.cart;

import java.util.UUID;

public record ChangeCartItemQuantityCommand(UUID customerId, UUID productId, int quantity) {}
