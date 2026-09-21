package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ProductCardInfo;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductCardQueryPort;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.Product;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class FindProductCardsUseCase implements ProductCardQueryPort {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public FindProductCardsUseCase(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
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
        Set<UUID> categoryIds = products.stream().map(Product::categoryId).collect(Collectors.toSet());
        Map<UUID, Category> categories = categoryRepository.findByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::id, Function.identity(), (left, right) -> left));
        return products.stream()
                .map(product -> toCard(product, categories.get(product.categoryId())))
                .toList();
    }

    private static ProductCardInfo toCard(Product product, Category category) {
        return new ProductCardInfo(
                product.id(),
                product.name(),
                product.brand(),
                product.price(),
                product.imageUrl(),
                product.categoryId(),
                product.status(),
                CatalogVisibility.isPubliclyVisible(product, category));
    }
}
