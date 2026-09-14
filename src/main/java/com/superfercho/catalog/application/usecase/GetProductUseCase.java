package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.Product;

public final class GetProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public GetProductUseCase(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductResult execute(GetProductCommand command) {
        Product product = productRepository
                .findById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));
        if (command.view() == CatalogView.PUBLIC) {
            Category category = categoryRepository.findById(product.categoryId()).orElse(null);
            if (!CatalogVisibility.isPubliclyVisible(product, category)) {
                throw new ProductNotFoundException(command.productId());
            }
        }
        return ProductResult.from(product);
    }
}
