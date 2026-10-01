package com.superfercho.orders.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Purchased value per customer inside a [from, to) period (CANCELLED orders excluded). */
public record AdminCustomerSalesRow(
        UUID customerId, String customerName, BigDecimal totalAmount, long orderCount) {}
