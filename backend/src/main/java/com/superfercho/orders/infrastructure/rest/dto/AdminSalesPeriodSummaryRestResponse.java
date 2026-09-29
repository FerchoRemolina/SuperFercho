package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.AdminSalesBucketResult;
import com.superfercho.orders.application.dto.AdminSalesPeriodSummaryResult;
import com.superfercho.orders.domain.model.SalesPeriodGranularity;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.List;

public record AdminSalesPeriodSummaryRestResponse(
        SalesPeriodGranularity granularity, List<AdminSalesBucketRestResponse> buckets) {

    public static AdminSalesPeriodSummaryRestResponse from(AdminSalesPeriodSummaryResult result) {
        return new AdminSalesPeriodSummaryRestResponse(
                result.granularity(),
                result.buckets().stream().map(AdminSalesBucketRestResponse::from).toList());
    }

    public record AdminSalesBucketRestResponse(
            Instant periodStart, String label, Money total, long orderCount) {

        public static AdminSalesBucketRestResponse from(AdminSalesBucketResult bucket) {
            return new AdminSalesBucketRestResponse(
                    bucket.periodStart(), bucket.label(), bucket.total(), bucket.orderCount());
        }
    }
}
