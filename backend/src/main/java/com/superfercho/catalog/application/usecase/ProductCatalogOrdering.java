package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductVariant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared catalog product ordering: name A-Z (case-insensitive), type/variant names as
 * tie-breakers, presentation descending within family, then id.
 */
final class ProductCatalogOrdering {

    private ProductCatalogOrdering() {}

    static Comparator<Product> comparator(
            Map<UUID, ProductType> typesById, Map<UUID, ProductVariant> variantsById) {
        return Comparator.comparing(Product::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(
                        product -> typeName(product, typesById), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(
                        product -> variantName(product, variantsById), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Product::presentation, Presentation.catalogOrder())
                .thenComparing(Product::id);
    }

    static List<Product> sorted(
            List<Product> products,
            Map<UUID, ProductType> typesById,
            Map<UUID, ProductVariant> variantsById) {
        return products.stream().sorted(comparator(typesById, variantsById)).toList();
    }

    private static String typeName(Product product, Map<UUID, ProductType> typesById) {
        ProductType type = typesById.get(product.productTypeId());
        return type == null ? "" : type.name();
    }

    private static String variantName(Product product, Map<UUID, ProductVariant> variantsById) {
        UUID variantId = product.productVariantId();
        if (variantId == null) {
            return "";
        }
        ProductVariant variant = variantsById.get(variantId);
        return variant == null ? "" : variant.name();
    }
}
