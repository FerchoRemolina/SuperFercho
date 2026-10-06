package com.superfercho.identity.infrastructure.persistence;

import com.superfercho.identity.application.dto.AdminCustomerRecordListItem;
import com.superfercho.identity.application.dto.AdminCustomerRecordSearchCriteria;
import com.superfercho.identity.application.dto.AdminCustomerRecordSortBy;
import com.superfercho.identity.application.dto.AdminCustomerRecordSortDir;
import com.superfercho.identity.application.dto.AdminCustomerRecordStatus;
import com.superfercho.identity.application.dto.AdminCustomerRecordsPage;
import com.superfercho.identity.application.dto.CustomerRegistrationBucketRow;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.infrastructure.persistence.mapper.CustomerRecordPersistenceMapper;
import com.superfercho.identity.infrastructure.persistence.repository.CustomerRecordJpaRepository;
import com.superfercho.platform.money.Money;
import com.superfercho.platform.time.BucketGranularity;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class CustomerRecordPersistenceAdapter implements CustomerRecordRepository {

    private final CustomerRecordJpaRepository customerRecordJpaRepository;
    private final CustomerRecordPersistenceMapper customerRecordPersistenceMapper;
    private final EntityManager entityManager;

    public CustomerRecordPersistenceAdapter(
            CustomerRecordJpaRepository customerRecordJpaRepository,
            CustomerRecordPersistenceMapper customerRecordPersistenceMapper,
            EntityManager entityManager) {
        this.customerRecordJpaRepository = customerRecordJpaRepository;
        this.customerRecordPersistenceMapper = customerRecordPersistenceMapper;
        this.entityManager = entityManager;
    }

    @Override
    public CustomerRecord save(CustomerRecord customerRecord) {
        try {
            return customerRecordPersistenceMapper.toDomain(customerRecordJpaRepository.saveAndFlush(
                    customerRecordPersistenceMapper.toEntity(customerRecord)));
        } catch (DataIntegrityViolationException exception) {
            throw IdentityConstraintViolationTranslator.translate(exception);
        }
    }

    @Override
    public Optional<CustomerRecord> findById(UUID id) {
        return customerRecordJpaRepository.findById(id).map(customerRecordPersistenceMapper::toDomain);
    }

    @Override
    public List<CustomerRecord> findAllByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return customerRecordJpaRepository.findAllById(ids).stream()
                .map(customerRecordPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<CustomerRecord> findByDocument(String documentType, String documentNumber) {
        return customerRecordJpaRepository
                .findByDocumentTypeAndDocumentNumber(documentType, documentNumber)
                .map(customerRecordPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByDocument(String documentType, String documentNumber) {
        return customerRecordJpaRepository.existsByDocumentTypeAndDocumentNumber(
                documentType, documentNumber);
    }

    @Override
    public List<CustomerRegistrationBucketRow> countRegistrationsByBucket(
            BucketGranularity granularity, Instant fromInclusive, Instant toExclusive) {
        String unit = switch (granularity) {
            case HOUR -> "hour";
            case DAY -> "day";
            case MONTH -> "month";
        };
        return customerRecordJpaRepository
                .countRegistrationsByBucket(unit, fromInclusive, toExclusive)
                .stream()
                .map(row -> new CustomerRegistrationBucketRow(toInstant(row[0]), (Long) row[1]))
                .toList();
    }

    @Override
    public long countRegistrationsBetween(Instant fromInclusive, Instant toExclusive) {
        return customerRecordJpaRepository.countRegistrationsBetween(fromInclusive, toExclusive);
    }

    /**
     * Paginated admin search over CustomerRecords. Aggregates accounts per
     * record (live/deleted) and commercial metrics across all the customer's
     * accounts (non-cancelled orders) via a LATERAL join. Order column and
     * direction come from the validated criteria (whitelisted in the use of
     * the adapter; never from raw client input).
     */
    @Override
    @Transactional(readOnly = true)
    public AdminCustomerRecordsPage searchRecords(AdminCustomerRecordSearchCriteria criteria) {
        String orderBySql = orderByExpression(criteria.sortBy(), criteria.sortDir());

        String dataSql =
                """
                SELECT cr.id,
                       cr.document_type,
                       cr.document_number,
                       cr.billing_first_name,
                       cr.billing_last_name,
                       cr.created_at,
                       cr.updated_at,
                       COUNT(u.id) AS account_count,
                       COUNT(u.id) FILTER (WHERE u.deleted_at IS NULL AND u.status = 'ACTIVE') AS active_accounts,
                       COUNT(u.id) FILTER (WHERE u.deleted_at IS NULL AND u.status = 'INACTIVE') AS inactive_accounts,
                       COUNT(u.id) FILTER (WHERE u.deleted_at IS NOT NULL) AS deleted_accounts,
                       COALESCE(SUM(oag.order_count), 0) AS order_count,
                       COALESCE(SUM(oag.total_spent), 0) AS total_spent,
                       MAX(oag.last_order_at) AS last_order_at,
                       (ARRAY_AGG(u.email ORDER BY u.created_at DESC) FILTER (WHERE u.deleted_at IS NULL))[1] AS email,
                       (ARRAY_AGG(u.phone ORDER BY u.created_at DESC) FILTER (WHERE u.deleted_at IS NULL))[1] AS phone
                  FROM identity.customer_records cr
                  LEFT JOIN identity.users u ON u.customer_record_id = cr.id
                  LEFT JOIN LATERAL (
                       SELECT COUNT(*) AS order_count,
                              COALESCE(SUM(o.total_amount), 0) AS total_spent,
                              MAX(o.created_at) AS last_order_at
                         FROM orders.orders o
                        WHERE o.customer_id = u.id
                          AND o.status <> 'CANCELLED'
                  ) oag ON TRUE
                 WHERE (:search = ''
                    OR cr.billing_first_name ILIKE :like
                    OR cr.billing_last_name ILIKE :like
                    OR cr.document_number ILIKE :like
                    OR u.email ILIKE :like
                    OR u.phone ILIKE :like)
                 GROUP BY cr.id, cr.document_type, cr.document_number,
                          cr.billing_first_name, cr.billing_last_name,
                          cr.created_at, cr.updated_at
                HAVING (:status = 'ALL'
                    OR (:status = 'ACTIVE'
                        AND COUNT(u.id) FILTER (WHERE u.deleted_at IS NULL AND u.status = 'ACTIVE') > 0)
                    OR (:status = 'INACTIVE'
                        AND COUNT(u.id) FILTER (WHERE u.deleted_at IS NULL AND u.status = 'ACTIVE') = 0
                        AND COUNT(u.id) FILTER (WHERE u.deleted_at IS NULL AND u.status = 'INACTIVE') > 0)
                    OR (:status = 'CLOSED'
                        AND COUNT(u.id) FILTER (WHERE u.deleted_at IS NULL) = 0
                        AND COUNT(u.id) FILTER (WHERE u.deleted_at IS NOT NULL) > 0)
                    OR (:status = 'NO_ACCOUNT'
                        AND COUNT(u.id) = 0))
                   /* CAST: JDBC null has no SQL type; without it PostgreSQL
                      fails with 42P18 on :hasPurchases IS NULL ($11). */
                   AND (CAST(:hasPurchases AS boolean) IS NULL
                    OR (CAST(:hasPurchases AS boolean) = TRUE
                        AND COALESCE(SUM(oag.order_count), 0) > 0)
                    OR (CAST(:hasPurchases AS boolean) = FALSE
                        AND COALESCE(SUM(oag.order_count), 0) = 0))
                 ORDER BY
                """
                + orderBySql
                + ", cr.id ASC LIMIT :limit OFFSET :offset";

        String countSql =
                """
                SELECT COUNT(*)
                  FROM (
                    SELECT cr.id
                      FROM identity.customer_records cr
                      LEFT JOIN identity.users u ON u.customer_record_id = cr.id
                      LEFT JOIN LATERAL (
                           SELECT COUNT(*) AS order_count
                         FROM orders.orders o
                            WHERE o.customer_id = u.id
                              AND o.status <> 'CANCELLED'
                      ) oag ON TRUE
                     WHERE (:search = ''
                        OR cr.billing_first_name ILIKE :like
                        OR cr.billing_last_name ILIKE :like
                        OR cr.document_number ILIKE :like
                        OR u.email ILIKE :like
                        OR u.phone ILIKE :like)
                     GROUP BY cr.id
                    HAVING (:status = 'ALL'
                        OR (:status = 'ACTIVE'
                            AND COUNT(u.id) FILTER (WHERE u.deleted_at IS NULL AND u.status = 'ACTIVE') > 0)
                        OR (:status = 'INACTIVE'
                            AND COUNT(u.id) FILTER (WHERE u.deleted_at IS NULL AND u.status = 'ACTIVE') = 0
                            AND COUNT(u.id) FILTER (WHERE u.deleted_at IS NULL AND u.status = 'INACTIVE') > 0)
                        OR (:status = 'CLOSED'
                            AND COUNT(u.id) FILTER (WHERE u.deleted_at IS NULL) = 0
                            AND COUNT(u.id) FILTER (WHERE u.deleted_at IS NOT NULL) > 0)
                        OR (:status = 'NO_ACCOUNT'
                            AND COUNT(u.id) = 0))
                       AND (CAST(:hasPurchases AS boolean) IS NULL
                        OR (CAST(:hasPurchases AS boolean) = TRUE
                            AND COALESCE(SUM(oag.order_count), 0) > 0)
                        OR (CAST(:hasPurchases AS boolean) = FALSE
                            AND COALESCE(SUM(oag.order_count), 0) = 0))
                  ) total
                """;

        var dataQuery = entityManager.createNativeQuery(dataSql);
        var countQuery = entityManager.createNativeQuery(countSql);
        for (jakarta.persistence.Query query :
                new jakarta.persistence.Query[] {dataQuery, countQuery}) {
            query.setParameter("search", criteria.search());
            query.setParameter("like", "%" + criteria.search() + "%");
            query.setParameter("status", criteria.status().name());
            query.setParameter("hasPurchases", criteria.hasPurchases());
        }
        dataQuery.setParameter("limit", criteria.size());
        dataQuery.setParameter("offset", (long) criteria.page() * criteria.size());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        long totalElements = ((Number) countQuery.getSingleResult()).longValue();

        List<AdminCustomerRecordListItem> items = rows.stream()
                .map(CustomerRecordPersistenceAdapter::toListItem)
                .toList();
        return new AdminCustomerRecordsPage(items, criteria.page(), criteria.size(), totalElements);
    }

    private static AdminCustomerRecordListItem toListItem(Object[] row) {
        long accountCount = asLong(row[7]);
        long activeAccounts = asLong(row[8]);
        long inactiveAccounts = asLong(row[9]);
        long deletedAccounts = asLong(row[10]);
        return new AdminCustomerRecordListItem(
                (UUID) row[0],
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                (String) row[14],
                (String) row[15],
                AdminCustomerRecordStatus.derive(activeAccounts, inactiveAccounts, deletedAccounts),
                accountCount,
                activeAccounts,
                inactiveAccounts,
                deletedAccounts,
                asLong(row[11]),
                Money.cop(asBigDecimal(row[12])),
                toInstant(row[13]),
                toInstant(row[5]),
                toInstant(row[6]));
    }

    private static long asLong(Object value) {
        return ((Number) value).longValue();
    }

    private static BigDecimal asBigDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        throw new IllegalArgumentException("Unsupported instant type: " + value.getClass());
    }

    private static String orderByExpression(
            AdminCustomerRecordSortBy sortBy, AdminCustomerRecordSortDir sortDir) {
        String direction = sortDir.name() + " NULLS LAST";
        return switch (sortBy) {
            case CREATED_AT -> "cr.created_at " + direction;
            case NAME -> "cr.billing_first_name " + direction + ", cr.billing_last_name " + direction;
            case DOCUMENT -> "cr.document_number " + direction;
            case ORDERS -> "COALESCE(SUM(oag.order_count), 0) " + direction;
            case TOTAL_SPENT -> "COALESCE(SUM(oag.total_spent), 0) " + direction;
            case LAST_ORDER_AT -> "MAX(oag.last_order_at) " + direction;
        };
    }
}
