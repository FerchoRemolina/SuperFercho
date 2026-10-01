package com.superfercho.identity.application.fakes;

import com.superfercho.identity.application.dto.CustomerRegistrationBucketRow;
import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.platform.time.BucketGranularity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryCustomerRecordRepository implements CustomerRecordRepository {

    private static final ZoneId ZONE = com.superfercho.platform.time.BusinessZone.BOGOTA;

    private final Map<UUID, CustomerRecord> byId = new LinkedHashMap<>();

    @Override
    public CustomerRecord save(CustomerRecord customerRecord) {
        boolean documentTaken = byId.values().stream()
                .anyMatch(existing -> existing.documentType().equals(customerRecord.documentType())
                        && existing.documentNumber().equals(customerRecord.documentNumber())
                        && !existing.id().equals(customerRecord.id()));
        if (documentTaken) {
            throw new DocumentAlreadyExistsException(
                    customerRecord.documentType(), customerRecord.documentNumber());
        }
        byId.put(customerRecord.id(), customerRecord);
        return customerRecord;
    }

    @Override
    public Optional<CustomerRecord> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<CustomerRecord> findByDocument(String documentType, String documentNumber) {
        return byId.values().stream()
                .filter(record -> record.documentType().equals(documentType)
                        && record.documentNumber().equals(documentNumber))
                .findFirst();
    }

    @Override
    public boolean existsByDocument(String documentType, String documentNumber) {
        return findByDocument(documentType, documentNumber).isPresent();
    }

    @Override
    public List<CustomerRegistrationBucketRow> countRegistrationsByBucket(
            BucketGranularity granularity, Instant fromInclusive, Instant toExclusive) {
        Map<Instant, Long> counts = new LinkedHashMap<>();
        for (CustomerRecord record : byId.values()) {
            Instant createdAt = record.createdAt();
            if (!createdAt.isBefore(fromInclusive) && createdAt.isBefore(toExclusive)) {
                counts.merge(bucketStart(granularity, createdAt), 1L, Long::sum);
            }
        }
        List<CustomerRegistrationBucketRow> rows = new ArrayList<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> rows.add(new CustomerRegistrationBucketRow(entry.getKey(), entry.getValue())));
        return List.copyOf(rows);
    }

    @Override
    public long countRegistrationsBetween(Instant fromInclusive, Instant toExclusive) {
        return byId.values().stream()
                .map(CustomerRecord::createdAt)
                .filter(createdAt -> !createdAt.isBefore(fromInclusive) && createdAt.isBefore(toExclusive))
                .count();
    }

    private static Instant bucketStart(BucketGranularity granularity, Instant instant) {
        return switch (granularity) {
            case HOUR -> instant.truncatedTo(ChronoUnit.HOURS);
            case DAY -> LocalDate.ofInstant(instant, ZONE).atStartOfDay(ZONE).toInstant();
            case MONTH -> YearMonth.from(LocalDate.ofInstant(instant, ZONE))
                    .atDay(1)
                    .atStartOfDay(ZONE)
                    .toInstant();
        };
    }
}
