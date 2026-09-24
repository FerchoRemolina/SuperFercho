package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
import java.util.UUID;

final class CatalogVisibility {

    private CatalogVisibility() {
    }

    /**
     * Public/sellable when Product, Category and ProductType are ACTIVE, and either
     * the product has no variant or the variant exists and is ACTIVE.
     */
    static boolean isPubliclyVisible(
            Product product, Category category, ProductType productType, ProductVariant productVariant) {
        if (product.status() != ProductStatus.ACTIVE) {
            return false;
        }
        if (category == null || category.status() != CategoryStatus.ACTIVE) {
            return false;
        }
        if (productType == null || productType.status() != ProductTypeStatus.ACTIVE) {
            return false;
        }
        UUID variantId = product.productVariantId();
        if (variantId == null) {
            return true;
        }
        return productVariant != null
                && variantId.equals(productVariant.id())
                && productVariant.status() == ProductVariantStatus.ACTIVE;
    }

    static boolean isPubliclyVisible(Category category) {
        return category.status() == CategoryStatus.ACTIVE;
    }
}
