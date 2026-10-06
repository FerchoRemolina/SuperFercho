package com.superfercho.identity.application.port;

import com.superfercho.identity.application.dto.AdminCustomerRecordListItem;
import com.superfercho.identity.application.dto.AdminCustomerRecordSearchCriteria;
import com.superfercho.identity.application.dto.AdminCustomerRecordsPage;
import com.superfercho.identity.application.dto.CustomerRegistrationBucketRow;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.platform.time.BucketGranularity;
import java.time.Instant;
import java.util.Collection;
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

    /**
     * Batch lookup of commercial records. Missing ids are silently omitted;
     * never returns null entries.
     */
    List<CustomerRecord> findAllByIds(Collection<UUID> ids);

    /**
     * Paginated admin search over CustomerRecords with aggregated account
     * status and commercial metrics (orders/spend across the customer's
     * accounts). Search matches billing name, document number and account
     * email/phone. Optional {@code hasPurchases} filters customers with or
     * without non-cancelled orders. Sorted inside the query (whitelisted).
     */
    AdminCustomerRecordsPage searchRecords(AdminCustomerRecordSearchCriteria criteria);
}
