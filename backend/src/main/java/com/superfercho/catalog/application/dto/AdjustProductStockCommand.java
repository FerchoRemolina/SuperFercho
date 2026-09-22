package com.superfercho.catalog.application.dto;

import java.util.UUID;

public record AdjustProductStockCommand(UUID productId, int stock) {}
