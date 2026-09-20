package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CreateProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public CreateProductUseCase(
            ProductRepository productRepository, CategoryRepository categoryRepository, Clock clock) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    public ProductResult execute(CreateProductCommand command) {
        categoryRepository
                .findById(command.categoryId())
                .orElseThrow(() -> new InvalidCategoryReferenceException(command.categoryId()));

        Instant now = clock.instant();
        Product product = Product.create(
                UUID.randomUUID(),
                command.categoryId(),
                command.barcode(),
                command.name(),
                command.brand(),
                command.description(),
                command.price(),
                command.stock(),
                command.imageUrl(),
                ProductStatus.ACTIVE,
                now,
                now);
        return ProductResult.from(productRepository.save(product));
    }
}
