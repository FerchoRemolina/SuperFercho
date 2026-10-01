package com.superfercho.orders.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Aggregated sales row for one [from, to) bucket (CANCELLED excluded upstream). */
public record AdminSalesBucketRow(Instant bucketStart, BigDecimal totalAmount, long orderCount) {}
