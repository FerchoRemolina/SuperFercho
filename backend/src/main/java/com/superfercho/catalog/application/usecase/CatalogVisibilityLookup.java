package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductVariant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Batch-loads Category / ProductType / ProductVariant maps for public visibility checks.
 */
final class CatalogVisibilityLookup {

    private final Map<UUID, Category> categoriesById;
    private final Map<UUID, ProductType> productTypesById;
    private final Map<UUID, ProductVariant> productVariantsById;

    private CatalogVisibilityLookup(
            Map<UUID, Category> categoriesById,
            Map<UUID, ProductType> productTypesById,
            Map<UUID, ProductVariant> productVariantsById) {
        this.categoriesById = categoriesById;
        this.productTypesById = productTypesById;
        this.productVariantsById = productVariantsById;
    }

    static CatalogVisibilityLookup load(
            Collection<Product> products,
            CategoryRepository categoryRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository) {
        Set<UUID> categoryIds = products.stream().map(Product::categoryId).collect(Collectors.toSet());
        Set<UUID> productTypeIds = products.stream().map(Product::productTypeId).collect(Collectors.toSet());
        Set<UUID> productVariantIds = products.stream()
                .map(Product::productVariantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, Category> categories = categoryRepository.findByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::id, Function.identity(), (left, right) -> left));
        Map<UUID, ProductType> productTypes = productTypeRepository.findByIds(productTypeIds).stream()
                .collect(Collectors.toMap(ProductType::id, Function.identity(), (left, right) -> left));
        Map<UUID, ProductVariant> productVariants = productVariantIds.isEmpty()
                ? Map.of()
                : productVariantRepository.findByIds(productVariantIds).stream()
                        .collect(Collectors.toMap(ProductVariant::id, Function.identity(), (left, right) -> left));

        return new CatalogVisibilityLookup(categories, productTypes, productVariants);
    }

    boolean isPubliclyVisible(Product product) {
        UUID variantId = product.productVariantId();
        return CatalogVisibility.isPubliclyVisible(
                product,
                categoriesById.get(product.categoryId()),
                productTypesById.get(product.productTypeId()),
                variantId == null ? null : productVariantsById.get(variantId));
    }
}
