package com.superfercho.catalog.infrastructure.persistence;

import com.superfercho.catalog.application.dto.StockDecrementResult;
import com.superfercho.catalog.application.dto.StockQuantity;
import com.superfercho.catalog.application.dto.UnavailableProduct;
import com.superfercho.catalog.application.port.InventoryPort;
import com.superfercho.catalog.infrastructure.persistence.repository.ProductJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("!test")
public class InventoryPersistenceAdapter implements InventoryPort {

    private final ProductJpaRepository productJpaRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public InventoryPersistenceAdapter(
            ProductJpaRepository productJpaRepository,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.productJpaRepository = productJpaRepository;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public StockDecrementResult decreaseStockAtomically(List<StockQuantity> items) {
        return transactionTemplate.execute(status -> {
            List<UnavailableProduct> unavailable = new ArrayList<>();
            Instant updatedAt = clock.instant();
            for (StockQuantity item : items) {
                int updated = productJpaRepository.decrementStockIfAvailable(
                        item.productId(), item.quantity(), updatedAt);
                if (updated == 0) {
                    unavailable.add(new UnavailableProduct(item.productId(), item.quantity()));
                }
            }
            if (!unavailable.isEmpty()) {
                status.setRollbackOnly();
                return StockDecrementResult.unavailable(unavailable);
            }
            return StockDecrementResult.success();
        });
    }

    @Override
    public void restoreStock(List<StockQuantity> items) {
        transactionTemplate.executeWithoutResult(status -> {
            Instant updatedAt = clock.instant();
            for (StockQuantity item : items) {
                productJpaRepository.incrementStock(item.productId(), item.quantity(), updatedAt);
            }
        });
    }
}
