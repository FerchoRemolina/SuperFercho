package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ProductPriceInfo;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductQueryPort;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductVariant;
import java.util.Optional;
import java.util.UUID;

public final class FindProductPriceUseCase implements ProductQueryPort {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductVariantRepository productVariantRepository;

    public FindProductPriceUseCase(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productTypeRepository = productTypeRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Override
    public Optional<ProductPriceInfo> findById(UUID productId) {
        return productRepository
                .findById(productId)
                .filter(this::isSellable)
                .map(product -> new ProductPriceInfo(product.id(), product.price()));
    }

    private boolean isSellable(Product product) {
        Category category = categoryRepository.findById(product.categoryId()).orElse(null);
        ProductType productType =
                productTypeRepository.findById(product.productTypeId()).orElse(null);
        ProductVariant productVariant = product.productVariantId() == null
                ? null
                : productVariantRepository.findById(product.productVariantId()).orElse(null);
        return CatalogVisibility.isPubliclyVisible(product, category, productType, productVariant);
    }
}
