package com.superfercho.identity.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * New customers for an arbitrary [from, to) period: total plus per-bucket series
 * (empty buckets included for chart continuity).
 */
public record AdminNewCustomersResult(long total, List<AdminNewCustomersBucket> buckets) {

    public record AdminNewCustomersBucket(Instant periodStart, String label, long count) {}
}
