package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.AdjustProductStockCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.exception.ProductStockConflictException;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Product;
import java.time.Clock;
import java.time.Instant;

public final class AdjustProductStockUseCase {

    private final ProductRepository productRepository;
    private final Clock clock;

    public AdjustProductStockUseCase(ProductRepository productRepository, Clock clock) {
        this.productRepository = productRepository;
        this.clock = clock;
    }

    public ProductResult execute(AdjustProductStockCommand command) {
        Product product = productRepository
                .findById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));
        Instant updatedAt = clock.instant();
        Product adjusted = product.changeStock(command.stock(), updatedAt);
        boolean updated = productRepository.adjustStockIfUnchanged(
                product.id(), product.stock(), adjusted.stock(), updatedAt);
        if (!updated) {
            if (productRepository.findById(product.id()).isEmpty()) {
                throw new ProductNotFoundException(product.id());
            }
            throw new ProductStockConflictException(product.id());
        }
        return ProductResult.from(adjusted);
    }
}
