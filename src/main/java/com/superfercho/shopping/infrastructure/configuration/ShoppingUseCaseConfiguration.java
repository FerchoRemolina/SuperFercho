package com.superfercho.shopping.infrastructure.configuration;

import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.application.service.CartApplicationService;
import com.superfercho.shopping.application.service.ShoppingListApplicationService;
import com.superfercho.shopping.infrastructure.clock.SystemClockAdapter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class ShoppingUseCaseConfiguration {

    @Bean
    ClockPort clockPort(Clock clock) {
        return new SystemClockAdapter(clock);
    }

    @Bean
    CartApplicationService cartApplicationService(
            CartRepositoryPort cartRepository, ProductCatalogPort productCatalogPort, ClockPort clockPort) {
        return new CartApplicationService(cartRepository, productCatalogPort, clockPort);
    }

    @Bean
    ShoppingListApplicationService shoppingListApplicationService(
            ShoppingListRepositoryPort shoppingListRepository,
            ProductCatalogPort productCatalogPort,
            ClockPort clockPort) {
        return new ShoppingListApplicationService(shoppingListRepository, productCatalogPort, clockPort);
    }
}
