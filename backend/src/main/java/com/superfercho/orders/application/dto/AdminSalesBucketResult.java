package com.superfercho.orders.application.dto;

import com.superfercho.platform.money.Money;
import java.time.Instant;

public record AdminSalesBucketResult(Instant periodStart, String label, Money total, long orderCount) {}
