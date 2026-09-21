package com.superfercho.shopping.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superfercho.platform.time.ClockConfiguration;
import com.superfercho.shopping.application.port.CurrentUserProvider;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.FavoriteRepositoryPort;
import com.superfercho.shopping.application.port.out.ProductCardCatalogPort;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.application.service.CartApplicationService;
import com.superfercho.shopping.application.service.FavoriteApplicationService;
import com.superfercho.shopping.application.service.ShoppingListApplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig({
    ClockConfiguration.class,
    ShoppingUseCaseConfiguration.class,
    ShoppingApplicationWiringTest.ShoppingTestPorts.class
})
class ShoppingApplicationWiringTest {

    @Configuration
    static class ShoppingTestPorts {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return Mockito.mock(CurrentUserProvider.class);
        }

        @Bean
        CartRepositoryPort cartRepositoryPort() {
            return Mockito.mock(CartRepositoryPort.class);
        }

        @Bean
        ShoppingListRepositoryPort shoppingListRepositoryPort() {
            return Mockito.mock(ShoppingListRepositoryPort.class);
        }

        @Bean
        ProductCatalogPort productCatalogPort() {
            return Mockito.mock(ProductCatalogPort.class);
        }

        @Bean
        FavoriteRepositoryPort favoriteRepositoryPort() {
            return Mockito.mock(FavoriteRepositoryPort.class);
        }

        @Bean
        ProductCardCatalogPort productCardCatalogPort() {
            return Mockito.mock(ProductCardCatalogPort.class);
        }
    }

    @Autowired
    private CartApplicationService cartApplicationService;

    @Autowired
    private ShoppingListApplicationService shoppingListApplicationService;

    @Autowired
    private FavoriteApplicationService favoriteApplicationService;

    @Autowired
    private CartRepositoryPort cartRepositoryPort;

    @Autowired
    private ShoppingListRepositoryPort shoppingListRepositoryPort;

    @Autowired
    private ProductCatalogPort productCatalogPort;

    @Autowired
    private ClockPort clockPort;

    @Test
    void shouldWireShoppingApplicationServices() {
        assertThat(cartApplicationService).isNotNull();
        assertThat(shoppingListApplicationService).isNotNull();
        assertThat(favoriteApplicationService).isNotNull();
        assertThat(cartRepositoryPort).isNotNull();
        assertThat(shoppingListRepositoryPort).isNotNull();
        assertThat(productCatalogPort).isNotNull();
        assertThat(clockPort).isNotNull();
        assertThat(clockPort.currentTime()).isNotNull();
    }
}
