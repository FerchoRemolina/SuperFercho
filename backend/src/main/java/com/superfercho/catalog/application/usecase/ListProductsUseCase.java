package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.ListProductsCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import java.util.List;
import java.util.UUID;

public final class ListProductsUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductVariantRepository productVariantRepository;

    public ListProductsUseCase(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productTypeRepository = productTypeRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public List<ProductResult> execute(ListProductsCommand command) {
        List<Product> products = load(command);
        CatalogVisibilityLookup lookup = CatalogVisibilityLookup.load(
                products, categoryRepository, productTypeRepository, productVariantRepository);
        List<Product> visible = command.view() == CatalogView.PUBLIC
                ? products.stream().filter(lookup::isPubliclyVisible).toList()
                : products;
        return ProductCatalogOrdering.sorted(
                        visible, lookup.productTypesById(), lookup.productVariantsById())
                .stream()
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
}
