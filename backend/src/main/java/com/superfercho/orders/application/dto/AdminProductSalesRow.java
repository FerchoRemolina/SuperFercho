package com.superfercho.orders.application.dto;

import java.util.UUID;

/** Units sold per product inside a [from, to) period (CANCELLED orders excluded). */
public record AdminProductSalesRow(UUID productId, String productName, long quantity) {}
