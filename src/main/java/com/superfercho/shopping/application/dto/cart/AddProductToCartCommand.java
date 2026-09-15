package com.superfercho.shopping.application.dto.cart;

import java.util.UUID;

public record AddProductToCartCommand(UUID customerId, UUID productId, int quantity) {}
