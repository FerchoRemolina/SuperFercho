package com.superfercho.orders.application.dto;

import com.superfercho.orders.domain.model.SalesPeriodGranularity;
import java.util.List;

public record AdminSalesPeriodSummaryResult(
        SalesPeriodGranularity granularity, List<AdminSalesBucketResult> buckets) {}
