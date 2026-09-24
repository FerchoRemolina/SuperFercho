package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductVariant;

public final class GetProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductVariantRepository productVariantRepository;

    public GetProductUseCase(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productTypeRepository = productTypeRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public ProductResult execute(GetProductCommand command) {
        Product product = productRepository
                .findById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));
        if (command.view() == CatalogView.PUBLIC) {
            Category category = categoryRepository.findById(product.categoryId()).orElse(null);
            ProductType productType =
                    productTypeRepository.findById(product.productTypeId()).orElse(null);
            ProductVariant productVariant = product.productVariantId() == null
                    ? null
                    : productVariantRepository.findById(product.productVariantId()).orElse(null);
            if (!CatalogVisibility.isPubliclyVisible(product, category, productType, productVariant)) {
                throw new ProductNotFoundException(command.productId());
            }
        }
        return ProductResult.from(product);
    }
}
