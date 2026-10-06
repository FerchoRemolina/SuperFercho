package com.superfercho.identity.application.fakes;

import com.superfercho.identity.application.dto.AdminCustomerRecordListItem;
import com.superfercho.identity.application.dto.AdminCustomerRecordSearchCriteria;
import com.superfercho.identity.application.dto.AdminCustomerRecordsPage;
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
import java.util.Collection;
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
    public List<CustomerRecord> findAllByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().map(byId::get).filter(record -> record != null).toList();
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

    @Override
    public AdminCustomerRecordsPage searchRecords(AdminCustomerRecordSearchCriteria criteria) {        String query = criteria.search().toLowerCase();
        List<AdminCustomerRecordListItem> items = byId.values().stream()
                .filter(record -> query.isEmpty()
                        || record.billingFirstName().toLowerCase().contains(query)
                        || record.billingLastName().toLowerCase().contains(query)
                        || record.documentNumber().contains(query))
                .map(record -> new AdminCustomerRecordListItem(
                        record.id(),
                        record.documentType(),
                        record.documentNumber(),
                        record.billingFirstName(),
                        record.billingLastName(),
                        null,
                        null,
                        com.superfercho.identity.application.dto.AdminCustomerRecordStatus.NO_ACCOUNT,
                        0,
                        0,
                        0,
                        0,
                        0,
                        com.superfercho.platform.money.Money.cop(java.math.BigDecimal.ZERO),
                        null,
                        record.createdAt(),
                        record.updatedAt()))
                .toList();
        long from = (long) criteria.page() * criteria.size();
        long to = Math.min(from + criteria.size(), items.size());
        List<AdminCustomerRecordListItem> slice =
                from >= items.size() ? List.of() : items.subList((int) from, (int) to);
        return new AdminCustomerRecordsPage(slice, criteria.page(), criteria.size(), items.size());
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
