package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ArchiveProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Product;
import java.time.Clock;

public final class ArchiveProductUseCase {

    private final ProductRepository productRepository;
    private final Clock clock;

    public ArchiveProductUseCase(ProductRepository productRepository, Clock clock) {
        this.productRepository = productRepository;
        this.clock = clock;
    }

    public ProductResult execute(ArchiveProductCommand command) {
        Product product = productRepository
                .findById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));
        return ProductResult.from(productRepository.save(product.archive(clock.instant())));
    }
}
