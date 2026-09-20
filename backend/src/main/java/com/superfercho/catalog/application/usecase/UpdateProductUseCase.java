package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.UpdateProductCommand;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Product;
import java.time.Clock;

public final class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public UpdateProductUseCase(
            ProductRepository productRepository, CategoryRepository categoryRepository, Clock clock) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    public ProductResult execute(UpdateProductCommand command) {
        Product product = productRepository
                .findById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));
        categoryRepository
                .findById(command.categoryId())
                .orElseThrow(() -> new InvalidCategoryReferenceException(command.categoryId()));

        Product updated = product.updateInformation(
                command.categoryId(),
                command.barcode(),
                command.name(),
                command.brand(),
                command.description(),
                command.imageUrl(),
                clock.instant());
        return ProductResult.from(productRepository.save(updated));
    }
}
