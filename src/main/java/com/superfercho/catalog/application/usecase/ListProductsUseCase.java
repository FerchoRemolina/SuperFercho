package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.ListProductsCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import java.util.List;
import java.util.UUID;

public final class ListProductsUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ListProductsUseCase(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductResult> execute(ListProductsCommand command) {
        return load(command).stream()
                .filter(product -> isVisible(product, command.view()))
                .map(ProductResult::from)
                .toList();
    }

    private List<Product> load(ListProductsCommand command) {
        UUID categoryId = command.categoryId();
        ProductStatus status =
                command.view() == CatalogView.PUBLIC ? ProductStatus.ACTIVE : command.status();
        if (categoryId != null && status != null) {
            return productRepository.findByCategoryIdAndStatus(categoryId, status);
        }
        if (categoryId != null) {
            return productRepository.findByCategoryId(categoryId);
        }
        if (status != null) {
            return productRepository.findByStatus(status);
        }
        return productRepository.findAll();
    }

    private boolean isVisible(Product product, CatalogView view) {
        if (view != CatalogView.PUBLIC) {
            return true;
        }
        return CatalogVisibility.isPubliclyVisible(
                product, categoryRepository.findById(product.categoryId()).orElse(null));
    }
}
