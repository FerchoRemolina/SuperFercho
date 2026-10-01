package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.AdminNewCustomersResult;
import java.time.Instant;
import java.util.List;

/**
 * New customers for an arbitrary [from, to) period, based on the real
 * registration (CustomerRecord creation) instant in America/Bogota.
 */
public record AdminNewCustomersRestResponse(long total, List<BucketRestResponse> buckets) {

    public static AdminNewCustomersRestResponse from(AdminNewCustomersResult result) {
        return new AdminNewCustomersRestResponse(
                result.total(),
                result.buckets().stream().map(BucketRestResponse::from).toList());
    }

    public record BucketRestResponse(Instant periodStart, String label, long count) {

        public static BucketRestResponse from(AdminNewCustomersResult.AdminNewCustomersBucket bucket) {
            return new BucketRestResponse(bucket.periodStart(), bucket.label(), bucket.count());
        }
    }
}
