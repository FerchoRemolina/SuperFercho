package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.SearchProductsCommand;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.Product;
import java.util.List;

public final class SearchProductsUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductVariantRepository productVariantRepository;

    public SearchProductsUseCase(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productTypeRepository = productTypeRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public List<ProductResult> execute(SearchProductsCommand command) {
        if (command.text() == null || command.text().isBlank()) {
            return List.of();
        }
        List<Product> products = productRepository.searchByNameBrandOrBarcode(command.text());
        if (command.view() != CatalogView.PUBLIC) {
            return products.stream().map(ProductResult::from).toList();
        }
        CatalogVisibilityLookup visibility = CatalogVisibilityLookup.load(
                products, categoryRepository, productTypeRepository, productVariantRepository);
        return products.stream()
                .filter(visibility::isPubliclyVisible)
                .map(ProductResult::from)
                .toList();
    }
}
