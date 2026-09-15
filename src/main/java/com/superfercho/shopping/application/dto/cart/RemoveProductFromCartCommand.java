package com.superfercho.shopping.application.dto.cart;

import java.util.UUID;

public record RemoveProductFromCartCommand(UUID customerId, UUID productId) {}
