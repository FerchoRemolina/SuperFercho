package com.superfercho.shopping.application.dto.cart;

import java.util.UUID;

public record ChangeCartItemQuantityCommand(UUID productId, int quantity) {}
