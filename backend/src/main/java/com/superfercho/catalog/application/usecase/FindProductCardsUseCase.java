package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ProductCardInfo;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductCardQueryPort;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.Product;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class FindProductCardsUseCase implements ProductCardQueryPort {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductVariantRepository productVariantRepository;

    public FindProductCardsUseCase(
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
    public List<ProductCardInfo> findCardsByIds(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<Product> products = productRepository.findByIds(productIds);
        if (products.isEmpty()) {
            return List.of();
        }
        CatalogVisibilityLookup visibility = CatalogVisibilityLookup.load(
                products, categoryRepository, productTypeRepository, productVariantRepository);
        return products.stream()
                .map(product -> toCard(product, visibility.isPubliclyVisible(product)))
                .toList();
    }

    private static ProductCardInfo toCard(Product product, boolean sellable) {
        return new ProductCardInfo(
                product.id(),
                product.name(),
                product.brand(),
                product.price(),
                product.imageUrl(),
                product.categoryId(),
                product.status(),
                sellable);
    }
}
