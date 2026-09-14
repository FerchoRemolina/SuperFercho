package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.SearchProductsCommand;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Product;
import java.util.List;

public final class SearchProductsUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public SearchProductsUseCase(
            ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductResult> execute(SearchProductsCommand command) {
        if (command.text() == null || command.text().isBlank()) {
            return List.of();
        }
        return productRepository.searchByNameBrandOrBarcode(command.text()).stream()
                .filter(product -> isVisible(product, command.view()))
                .map(ProductResult::from)
                .toList();
    }

    private boolean isVisible(Product product, CatalogView view) {
        if (view != CatalogView.PUBLIC) {
            return true;
        }
        return CatalogVisibility.isPubliclyVisible(
                product, categoryRepository.findById(product.categoryId()).orElse(null));
    }
}
