package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.AdminSalesAnalyticsResult;
import com.superfercho.orders.application.dto.AdminSalesBucketResult;
import com.superfercho.orders.application.dto.AdminSalesPeriodSummaryResult;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.List;

/**
 * Unified shape for the sales dashboard: legacy rolling windows (DAY, WEEK,
 * MONTH, YEAR) and arbitrary [from, to) periods (HOUR, DAY, MONTH buckets).
 */
public record AdminSalesPeriodSummaryRestResponse(String granularity, List<AdminSalesBucketRestResponse> buckets) {

    public static AdminSalesPeriodSummaryRestResponse from(AdminSalesPeriodSummaryResult result) {
        return new AdminSalesPeriodSummaryRestResponse(
                result.granularity().name(),
                result.buckets().stream().map(AdminSalesBucketRestResponse::from).toList());
    }

    public static AdminSalesPeriodSummaryRestResponse from(AdminSalesAnalyticsResult result) {
        return new AdminSalesPeriodSummaryRestResponse(
                result.granularity().name(),
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
