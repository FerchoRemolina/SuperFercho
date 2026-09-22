package com.superfercho.catalog.infrastructure.configuration;

import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductCardQueryPort;
import com.superfercho.catalog.application.port.ProductQueryPort;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.usecase.ActivateCategoryUseCase;
import com.superfercho.catalog.application.usecase.ActivateProductUseCase;
import com.superfercho.catalog.application.usecase.AdjustProductStockUseCase;
import com.superfercho.catalog.application.usecase.ChangeProductPriceUseCase;
import com.superfercho.catalog.application.usecase.CreateCategoryUseCase;
import com.superfercho.catalog.application.usecase.CreateProductUseCase;
import com.superfercho.catalog.application.usecase.DeactivateCategoryUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductUseCase;
import com.superfercho.catalog.application.usecase.FindProductCardsUseCase;
import com.superfercho.catalog.application.usecase.FindProductPriceUseCase;
import com.superfercho.catalog.application.usecase.GetCategoryUseCase;
import com.superfercho.catalog.application.usecase.GetProductUseCase;
import com.superfercho.catalog.application.usecase.ListCategoriesUseCase;
import com.superfercho.catalog.application.usecase.ListProductsUseCase;
import com.superfercho.catalog.application.usecase.SearchProductsUseCase;
import com.superfercho.catalog.application.usecase.UpdateCategoryUseCase;
import com.superfercho.catalog.application.usecase.UpdateProductUseCase;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class CatalogUseCaseConfiguration {

    @Bean
    ProductQueryPort productQueryPort(ProductRepository productRepository, CategoryRepository categoryRepository) {
        return new FindProductPriceUseCase(productRepository, categoryRepository);
    }

    @Bean
    ProductCardQueryPort productCardQueryPort(
            ProductRepository productRepository, CategoryRepository categoryRepository) {
        return new FindProductCardsUseCase(productRepository, categoryRepository);
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
    CreateProductUseCase createProductUseCase(
            ProductRepository productRepository, CategoryRepository categoryRepository, Clock clock) {
        return new CreateProductUseCase(productRepository, categoryRepository, clock);
    }

    @Bean
    GetProductUseCase getProductUseCase(
            ProductRepository productRepository, CategoryRepository categoryRepository) {
        return new GetProductUseCase(productRepository, categoryRepository);
    }

    @Bean
    ListProductsUseCase listProductsUseCase(
            ProductRepository productRepository, CategoryRepository categoryRepository) {
        return new ListProductsUseCase(productRepository, categoryRepository);
    }

    @Bean
    SearchProductsUseCase searchProductsUseCase(
            ProductRepository productRepository, CategoryRepository categoryRepository) {
        return new SearchProductsUseCase(productRepository, categoryRepository);
    }

    @Bean
    UpdateProductUseCase updateProductUseCase(
            ProductRepository productRepository, CategoryRepository categoryRepository, Clock clock) {
        return new UpdateProductUseCase(productRepository, categoryRepository, clock);
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
    ChangeProductPriceUseCase changeProductPriceUseCase(ProductRepository productRepository, Clock clock) {
        return new ChangeProductPriceUseCase(productRepository, clock);
    }

    @Bean
    AdjustProductStockUseCase adjustProductStockUseCase(ProductRepository productRepository, Clock clock) {
        return new AdjustProductStockUseCase(productRepository, clock);
    }
}
