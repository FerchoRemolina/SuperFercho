package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ProductPriceInfo;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductQueryPort;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.Product;
import java.util.Optional;
import java.util.UUID;

public final class FindProductPriceUseCase implements ProductQueryPort {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public FindProductPriceUseCase(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
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
        return CatalogVisibility.isPubliclyVisible(product, category);
    }
}
