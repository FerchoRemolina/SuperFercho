package com.superfercho.orders.application.dto;

/**
 * One row of the admin orders list: the order plus the enriched commercial
 * identity of its customer. {@code customer} is null when the account cannot
 * be resolved (e.g. hard-deleted); the row still renders with the raw
 * customerId.
 */
public record AdminOrderListItemResult(OrderResult order, CustomerDirectoryEntry customer) {}
