package com.superfercho.shopping.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superfercho.platform.time.ClockConfiguration;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.application.service.CartApplicationService;
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
    }

    @Autowired
    private CartApplicationService cartApplicationService;

    @Autowired
    private ShoppingListApplicationService shoppingListApplicationService;

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
        assertThat(cartRepositoryPort).isNotNull();
        assertThat(shoppingListRepositoryPort).isNotNull();
        assertThat(productCatalogPort).isNotNull();
        assertThat(clockPort).isNotNull();
        assertThat(clockPort.currentTime()).isNotNull();
    }
}
