package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.DeactivateProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Product;
import java.time.Clock;

public final class DeactivateProductUseCase {

    private final ProductRepository productRepository;
    private final Clock clock;

    public DeactivateProductUseCase(ProductRepository productRepository, Clock clock) {
        this.productRepository = productRepository;
        this.clock = clock;
    }

    public ProductResult execute(DeactivateProductCommand command) {
        Product product = productRepository
                .findById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));
        return ProductResult.from(productRepository.save(product.deactivate(clock.instant())));
    }
}
