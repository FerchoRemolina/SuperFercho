package com.superfercho.catalog.infrastructure.configuration;

import com.superfercho.catalog.application.port.ProductQueryPort;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.usecase.FindProductPriceUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class CatalogUseCaseConfiguration {

    @Bean
    ProductQueryPort productQueryPort(ProductRepository productRepository) {
        return new FindProductPriceUseCase(productRepository);
    }
}
