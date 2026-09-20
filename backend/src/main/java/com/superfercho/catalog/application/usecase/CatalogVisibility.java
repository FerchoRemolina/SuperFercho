package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;

final class CatalogVisibility {

    private CatalogVisibility() {
    }

    static boolean isPubliclyVisible(Product product, Category category) {
        return product.status() == ProductStatus.ACTIVE
                && category != null
                && category.status() == CategoryStatus.ACTIVE;
    }

    static boolean isPubliclyVisible(Category category) {
        return category.status() == CategoryStatus.ACTIVE;
    }
}
