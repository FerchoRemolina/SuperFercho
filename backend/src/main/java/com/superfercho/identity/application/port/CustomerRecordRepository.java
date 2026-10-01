package com.superfercho.identity.application.port;

import com.superfercho.identity.application.dto.CustomerRegistrationBucketRow;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.platform.time.BucketGranularity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRecordRepository {

    CustomerRecord save(CustomerRecord customerRecord);

    Optional<CustomerRecord> findById(UUID id);

    Optional<CustomerRecord> findByDocument(String documentType, String documentNumber);

    boolean existsByDocument(String documentType, String documentNumber);

    /**
     * Real registrations (CustomerRecord creation) inside
     * {@code [fromInclusive, toExclusive)}, grouped by {@code granularity} buckets
     * following America/Bogota.
     */
    List<CustomerRegistrationBucketRow> countRegistrationsByBucket(
            BucketGranularity granularity, Instant fromInclusive, Instant toExclusive);

    /** Real registrations inside {@code [fromInclusive, toExclusive)}. */
    long countRegistrationsBetween(Instant fromInclusive, Instant toExclusive);
}
