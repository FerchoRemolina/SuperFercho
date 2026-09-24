package com.superfercho.orders.infrastructure.persistence;

import com.superfercho.orders.application.dto.IdempotencyRecord;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.infrastructure.persistence.entity.CheckoutIdempotencyJpaEntity;
import com.superfercho.orders.infrastructure.persistence.mapper.CheckoutIdempotencyPersistenceMapper;
import com.superfercho.orders.infrastructure.persistence.repository.CheckoutIdempotencyJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class IdempotencyPersistenceAdapter implements IdempotencyPort {

    /**
     * Transaction-scoped PostgreSQL advisory lock for (customer_id, idempotency_key).
     *
     * <p>pg_advisory_xact_lock(bigint) needs a single 64-bit key. The identity is hashed with
     * PostgreSQL {@code hashtextextended(text, bigint)} using a fixed seed of 0:
     *
     * <pre>
     * hashtextextended(
     *     concat(cast(customer_id as text), chr(31), idempotency_key),
     *     0)
     * </pre>
     *
     * <p>chr(31) (unit separator) prevents ambiguous concatenations of UUID text and key.
     * The hash is deterministic for a given PostgreSQL major version; it is not a random nonce.
     * The lock is held only until the current PostgreSQL transaction commits or rolls back.
     * This adapter does not open that transaction; Infrastructure must wrap CheckoutUseCase.execute().
     */
    static final String ADVISORY_LOCK_SQL =
            """
            select pg_advisory_xact_lock(
                hashtextextended(concat(cast(? as text), chr(31), cast(? as text)), 0)
            )
            """;

    private final CheckoutIdempotencyJpaRepository checkoutIdempotencyJpaRepository;
    private final CheckoutIdempotencyPersistenceMapper checkoutIdempotencyPersistenceMapper;
    private final JdbcTemplate jdbcTemplate;

    public IdempotencyPersistenceAdapter(
            CheckoutIdempotencyJpaRepository checkoutIdempotencyJpaRepository,
            CheckoutIdempotencyPersistenceMapper checkoutIdempotencyPersistenceMapper,
            JdbcTemplate jdbcTemplate) {
        this.checkoutIdempotencyJpaRepository = checkoutIdempotencyJpaRepository;
        this.checkoutIdempotencyPersistenceMapper = checkoutIdempotencyPersistenceMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<IdempotencyRecord> find(String key, UUID customerId) {
        acquireTransactionLock(customerId, key);
        return checkoutIdempotencyJpaRepository
                .findByCustomerIdAndIdempotencyKey(customerId, key)
                .map(checkoutIdempotencyPersistenceMapper::toRecord);
    }

    @Override
    public void save(IdempotencyRecord record) {
        UUID id = checkoutIdempotencyJpaRepository
                .findByCustomerIdAndIdempotencyKey(record.customerId(), record.key())
                .map(existing -> reuseExpiredRowId(existing, record))
                .orElseGet(UUID::randomUUID);
        checkoutIdempotencyJpaRepository.saveAndFlush(
                checkoutIdempotencyPersistenceMapper.toEntity(id, record));
    }

    @Override
    public void deleteAllByCustomerId(UUID customerId) {
        checkoutIdempotencyJpaRepository.deleteAllByCustomerId(customerId);
    }

    private void acquireTransactionLock(UUID customerId, String key) {
        jdbcTemplate.query(ADVISORY_LOCK_SQL, rs -> null, customerId.toString(), key);
    }

    private static UUID reuseExpiredRowId(CheckoutIdempotencyJpaEntity existing, IdempotencyRecord record) {
        if (!record.createdAt().isAfter(existing.getExpiresAt())) {
            throw new IllegalStateException(
                    "Cannot overwrite a non-expired checkout idempotency record");
        }
        return existing.getId();
    }
}
