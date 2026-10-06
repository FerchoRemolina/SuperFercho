package com.superfercho.orders.application.dto;

/**
 * Admin order detail: the full order plus the enriched commercial identity of
 * its customer (null when the account cannot be resolved).
 */
public record AdminOrderDetailResult(OrderResult order, CustomerDirectoryEntry customer) {}
