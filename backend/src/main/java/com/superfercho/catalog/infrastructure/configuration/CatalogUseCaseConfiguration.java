package com.superfercho.catalog.infrastructure.configuration;

import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductCardQueryPort;
import com.superfercho.catalog.application.port.ProductQueryPort;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.application.usecase.ActivateCategoryUseCase;
import com.superfercho.catalog.application.usecase.ActivateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.ActivateProductUseCase;
import com.superfercho.catalog.application.usecase.ActivateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.AdjustProductStockUseCase;
import com.superfercho.catalog.application.usecase.ArchiveProductUseCase;
import com.superfercho.catalog.application.usecase.ChangeProductPriceUseCase;
import com.superfercho.catalog.application.usecase.CreateCategoryUseCase;
import com.superfercho.catalog.application.usecase.CreateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.CreateProductUseCase;
import com.superfercho.catalog.application.usecase.CreateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.DeactivateCategoryUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.FindProductCardsUseCase;
import com.superfercho.catalog.application.usecase.FindProductPriceUseCase;
import com.superfercho.catalog.application.usecase.GetCategoryUseCase;
import com.superfercho.catalog.application.usecase.GetProductTypeUseCase;
import com.superfercho.catalog.application.usecase.GetProductUseCase;
import com.superfercho.catalog.application.usecase.GetProductVariantUseCase;
import com.superfercho.catalog.application.usecase.ListCategoriesUseCase;
import com.superfercho.catalog.application.usecase.ListProductTypesUseCase;
import com.superfercho.catalog.application.usecase.ListProductVariantsUseCase;
import com.superfercho.catalog.application.usecase.ListProductsUseCase;
import com.superfercho.catalog.application.usecase.RestoreProductUseCase;
import com.superfercho.catalog.application.usecase.SearchProductsUseCase;
import com.superfercho.catalog.application.usecase.UpdateCategoryUseCase;
import com.superfercho.catalog.application.usecase.UpdateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.UpdateProductUseCase;
import com.superfercho.catalog.application.usecase.UpdateProductVariantUseCase;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class CatalogUseCaseConfiguration {

    @Bean
    ProductQueryPort productQueryPort(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository) {
        return new FindProductPriceUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
    }

    @Bean
    ProductCardQueryPort productCardQueryPort(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository) {
        return new FindProductCardsUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
    }

    @Bean
    CreateCategoryUseCase createCategoryUseCase(CategoryRepository categoryRepository, Clock clock) {
        return new CreateCategoryUseCase(categoryRepository, clock);
    }

    @Bean
    GetCategoryUseCase getCategoryUseCase(CategoryRepository categoryRepository) {
        return new GetCategoryUseCase(categoryRepository);
    }

    @Bean
    ListCategoriesUseCase listCategoriesUseCase(CategoryRepository categoryRepository) {
        return new ListCategoriesUseCase(categoryRepository);
    }

    @Bean
    UpdateCategoryUseCase updateCategoryUseCase(CategoryRepository categoryRepository, Clock clock) {
        return new UpdateCategoryUseCase(categoryRepository, clock);
    }

    @Bean
    ActivateCategoryUseCase activateCategoryUseCase(CategoryRepository categoryRepository, Clock clock) {
        return new ActivateCategoryUseCase(categoryRepository, clock);
    }

    @Bean
    DeactivateCategoryUseCase deactivateCategoryUseCase(CategoryRepository categoryRepository, Clock clock) {
        return new DeactivateCategoryUseCase(categoryRepository, clock);
    }

    @Bean
    CreateProductTypeUseCase createProductTypeUseCase(
            ProductTypeRepository productTypeRepository, CategoryRepository categoryRepository, Clock clock) {
        return new CreateProductTypeUseCase(productTypeRepository, categoryRepository, clock);
    }

    @Bean
    GetProductTypeUseCase getProductTypeUseCase(ProductTypeRepository productTypeRepository) {
        return new GetProductTypeUseCase(productTypeRepository);
    }

    @Bean
    ListProductTypesUseCase listProductTypesUseCase(ProductTypeRepository productTypeRepository) {
        return new ListProductTypesUseCase(productTypeRepository);
    }

    @Bean
    UpdateProductTypeUseCase updateProductTypeUseCase(ProductTypeRepository productTypeRepository, Clock clock) {
        return new UpdateProductTypeUseCase(productTypeRepository, clock);
    }

    @Bean
    ActivateProductTypeUseCase activateProductTypeUseCase(
            ProductTypeRepository productTypeRepository, Clock clock) {
        return new ActivateProductTypeUseCase(productTypeRepository, clock);
    }

    @Bean
    DeactivateProductTypeUseCase deactivateProductTypeUseCase(
            ProductTypeRepository productTypeRepository, Clock clock) {
        return new DeactivateProductTypeUseCase(productTypeRepository, clock);
    }

    @Bean
    CreateProductVariantUseCase createProductVariantUseCase(
            ProductVariantRepository productVariantRepository,
            ProductTypeRepository productTypeRepository,
            Clock clock) {
        return new CreateProductVariantUseCase(productVariantRepository, productTypeRepository, clock);
    }

    @Bean
    GetProductVariantUseCase getProductVariantUseCase(ProductVariantRepository productVariantRepository) {
        return new GetProductVariantUseCase(productVariantRepository);
    }

    @Bean
    ListProductVariantsUseCase listProductVariantsUseCase(ProductVariantRepository productVariantRepository) {
        return new ListProductVariantsUseCase(productVariantRepository);
    }

    @Bean
    UpdateProductVariantUseCase updateProductVariantUseCase(
            ProductVariantRepository productVariantRepository, Clock clock) {
        return new UpdateProductVariantUseCase(productVariantRepository, clock);
    }

    @Bean
    ActivateProductVariantUseCase activateProductVariantUseCase(
            ProductVariantRepository productVariantRepository, Clock clock) {
        return new ActivateProductVariantUseCase(productVariantRepository, clock);
    }

    @Bean
    DeactivateProductVariantUseCase deactivateProductVariantUseCase(
            ProductVariantRepository productVariantRepository, Clock clock) {
        return new DeactivateProductVariantUseCase(productVariantRepository, clock);
    }

    @Bean
    CreateProductUseCase createProductUseCase(
            ProductRepository productRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository,
            Clock clock) {
        return new CreateProductUseCase(
                productRepository, productTypeRepository, productVariantRepository, clock);
    }

    @Bean
    GetProductUseCase getProductUseCase(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository) {
        return new GetProductUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
    }

    @Bean
    ListProductsUseCase listProductsUseCase(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository) {
        return new ListProductsUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
    }

    @Bean
    SearchProductsUseCase searchProductsUseCase(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository) {
        return new SearchProductsUseCase(
                productRepository, categoryRepository, productTypeRepository, productVariantRepository);
    }

    @Bean
    UpdateProductUseCase updateProductUseCase(
            ProductRepository productRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository,
            Clock clock) {
        return new UpdateProductUseCase(
                productRepository, productTypeRepository, productVariantRepository, clock);
    }

    @Bean
    ActivateProductUseCase activateProductUseCase(ProductRepository productRepository, Clock clock) {
        return new ActivateProductUseCase(productRepository, clock);
    }

    @Bean
    DeactivateProductUseCase deactivateProductUseCase(ProductRepository productRepository, Clock clock) {
        return new DeactivateProductUseCase(productRepository, clock);
    }

    @Bean
    ArchiveProductUseCase archiveProductUseCase(ProductRepository productRepository, Clock clock) {
        return new ArchiveProductUseCase(productRepository, clock);
    }

    @Bean
    RestoreProductUseCase restoreProductUseCase(ProductRepository productRepository, Clock clock) {
        return new RestoreProductUseCase(productRepository, clock);
    }

    @Bean
    ChangeProductPriceUseCase changeProductPriceUseCase(ProductRepository productRepository, Clock clock) {
        return new ChangeProductPriceUseCase(productRepository, clock);
    }

    @Bean
    AdjustProductStockUseCase adjustProductStockUseCase(ProductRepository productRepository, Clock clock) {
        return new AdjustProductStockUseCase(productRepository, clock);
    }
}
