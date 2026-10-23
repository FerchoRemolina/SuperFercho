package com.superfercho.shopping.infrastructure.persistence;

import com.superfercho.shopping.application.dto.cart.CartAddOperation;
import com.superfercho.shopping.application.port.out.CartAddOperationPort;
import com.superfercho.shopping.infrastructure.persistence.mapper.CartAddOperationPersistenceMapper;
import com.superfercho.shopping.infrastructure.persistence.repository.CartAddOperationJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CartAddOperationPersistenceAdapter implements CartAddOperationPort {

    /**
     * Transaction-scoped PostgreSQL advisory lock for {@code operation_id}.
     *
     * <p>This adapter does not open the transaction; Infrastructure must wrap the
     * idempotent cart add in a single local transaction so the lock is held until
     * commit or rollback.
     */
    static final String ADVISORY_LOCK_SQL =
            """
            select pg_advisory_xact_lock(
                hashtextextended(cast(? as text), 0)
            )
            """;

    private final CartAddOperationJpaRepository cartAddOperationJpaRepository;
    private final CartAddOperationPersistenceMapper cartAddOperationPersistenceMapper;
    private final JdbcTemplate jdbcTemplate;

    public CartAddOperationPersistenceAdapter(
            CartAddOperationJpaRepository cartAddOperationJpaRepository,
            CartAddOperationPersistenceMapper cartAddOperationPersistenceMapper,
            JdbcTemplate jdbcTemplate) {
        this.cartAddOperationJpaRepository = cartAddOperationJpaRepository;
        this.cartAddOperationPersistenceMapper = cartAddOperationPersistenceMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CartAddOperation> find(UUID operationId) {
        if (operationId == null) {
            return Optional.empty();
        }
        return cartAddOperationJpaRepository
                .findByOperationId(operationId)
                .map(cartAddOperationPersistenceMapper::toDomain);
    }

    @Override
    public Optional<CartAddOperation> findAndLock(UUID operationId) {
        if (operationId == null) {
            return Optional.empty();
        }
        jdbcTemplate.query(ADVISORY_LOCK_SQL, rs -> null, operationId.toString());
        return find(operationId);
    }

    @Override
    public void save(CartAddOperation operation) {
        try {
            cartAddOperationJpaRepository.saveAndFlush(cartAddOperationPersistenceMapper.toEntity(operation));
        } catch (DataIntegrityViolationException exception) {
            throw ShoppingConstraintViolationTranslator.translate(exception);
        }
    }

    @Override
    public void deleteAllByCustomerId(UUID customerId) {
        cartAddOperationJpaRepository.deleteAllByCustomerId(customerId);
    }
}
