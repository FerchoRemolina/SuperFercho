package com.superfercho.orders.application.dto;

import com.superfercho.platform.time.BucketGranularity;
import java.util.List;

/** Sales series for an arbitrary [from, to) period with empty buckets filled. */
public record AdminSalesAnalyticsResult(BucketGranularity granularity, List<AdminSalesBucketResult> buckets) {}
