package com.superfercho.shopping.infrastructure.configuration;

import com.superfercho.shopping.application.port.CurrentUserProvider;
import com.superfercho.shopping.application.port.in.ApplyIdempotentCartAddUseCase;
import com.superfercho.shopping.application.port.out.CartAddOperationPort;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.FavoriteRepositoryPort;
import com.superfercho.shopping.application.port.out.ProductCardCatalogPort;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.application.service.CartApplicationService;
import com.superfercho.shopping.application.service.FavoriteApplicationService;
import com.superfercho.shopping.application.service.ShoppingListApplicationService;
import com.superfercho.shopping.infrastructure.clock.SystemClockAdapter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Profile("!test")
public class ShoppingUseCaseConfiguration {

    @Bean
    ClockPort clockPort(Clock clock) {
        return new SystemClockAdapter(clock);
    }

    @Bean
    CartApplicationService cartApplicationService(
            CurrentUserProvider currentUserProvider,
            CartRepositoryPort cartRepository,
            ShoppingListRepositoryPort shoppingListRepository,
            ProductCatalogPort productCatalogPort,
            ClockPort clockPort,
            CartAddOperationPort cartAddOperationPort) {
        return new CartApplicationService(
                currentUserProvider,
                cartRepository,
                shoppingListRepository,
                productCatalogPort,
                clockPort,
                cartAddOperationPort);
    }

    @Bean
    ApplyIdempotentCartAddUseCase applyIdempotentCartAddUseCase(
            CartApplicationService cartApplicationService, PlatformTransactionManager transactionManager) {
        return new TransactionalApplyIdempotentCartAddUseCase(
                cartApplicationService, new TransactionTemplate(transactionManager));
    }

    @Bean
    ShoppingListApplicationService shoppingListApplicationService(
            CurrentUserProvider currentUserProvider,
            ShoppingListRepositoryPort shoppingListRepository,
            ProductCatalogPort productCatalogPort,
            ClockPort clockPort) {
        return new ShoppingListApplicationService(
                currentUserProvider, shoppingListRepository, productCatalogPort, clockPort);
    }

    @Bean
    FavoriteApplicationService favoriteApplicationService(
            CurrentUserProvider currentUserProvider,
            FavoriteRepositoryPort favoriteRepository,
            ProductCatalogPort productCatalogPort,
            ProductCardCatalogPort productCardCatalogPort,
            ClockPort clockPort) {
        return new FavoriteApplicationService(
                currentUserProvider,
                favoriteRepository,
                productCatalogPort,
                productCardCatalogPort,
                clockPort);
    }
}
