package com.superfercho.orders.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superfercho.catalog.application.port.InventoryPort;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.CurrentUserProvider;
import com.superfercho.orders.application.port.CustomerAddressPort;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.application.port.PreviewCustomerExclusionPort;
import com.superfercho.orders.application.port.ProductCatalogPort;
import com.superfercho.orders.application.port.ShoppingCartPort;
import com.superfercho.orders.application.port.in.CancelOrderUseCase;
import com.superfercho.orders.application.port.in.CheckoutUseCase;
import com.superfercho.orders.application.usecase.AutoConfirmPendingOrdersUseCase;
import com.superfercho.orders.application.usecase.GetAdminOrderUseCase;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.application.usecase.ListAdminOrdersUseCase;
import com.superfercho.orders.application.usecase.ListOrdersUseCase;
import com.superfercho.orders.application.usecase.UpdateOrderStatusUseCase;
import com.superfercho.orders.infrastructure.clock.SystemClockAdapter;
import com.superfercho.platform.time.ClockConfiguration;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

@SpringJUnitConfig({
    ClockConfiguration.class,
    OrdersUseCaseConfiguration.class,
    OrdersUseCaseConfigurationTest.OrdersTestPorts.class
})
class OrdersUseCaseConfigurationTest {

    @Configuration
    static class OrdersTestPorts {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return Mockito.mock(CurrentUserProvider.class);
        }

        @Bean
        ShoppingCartPort shoppingCartPort() {
            return Mockito.mock(ShoppingCartPort.class);
        }

        @Bean
        CustomerAddressPort customerAddressPort() {
            return Mockito.mock(CustomerAddressPort.class);
        }

        @Bean
        ProductCatalogPort productCatalogPort() {
            return Mockito.mock(ProductCatalogPort.class);
        }

        @Bean
        InventoryPort inventoryPort() {
            return Mockito.mock(InventoryPort.class);
        }

        @Bean
        PaymentPort paymentPort() {
            return Mockito.mock(PaymentPort.class);
        }

        @Bean
        OrderRepository orderRepository() {
            return Mockito.mock(OrderRepository.class);
        }

        @Bean
        IdempotencyPort idempotencyPort() {
            return Mockito.mock(IdempotencyPort.class);
        }

        @Bean
        PreviewCustomerExclusionPort previewCustomerExclusionPort() {
            return customerId -> false;
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return Mockito.mock(PlatformTransactionManager.class);
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Clock clock;

    @Autowired
    private ClockProvider ordersClockProvider;

    @Autowired
    private TransactionalCheckoutUseCase transactionalCheckoutUseCase;

    @Autowired
    private TransactionalCancelOrderUseCase transactionalCancelOrderUseCase;

    @Autowired
    private GetOrderUseCase getOrderUseCase;

    @Autowired
    private ListOrdersUseCase listOrdersUseCase;

    @Autowired
    private GetAdminOrderUseCase getAdminOrderUseCase;

    @Autowired
    private ListAdminOrdersUseCase listAdminOrdersUseCase;

    @Autowired
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @Autowired
    private AutoConfirmPendingOrdersUseCase autoConfirmPendingOrdersUseCase;

    @Test
    void shouldWireOrdersUseCasesWithoutExposingRawTransactionalDelegates() {
        assertThat(ordersClockProvider).isInstanceOf(SystemClockAdapter.class);
        assertThat(ordersClockProvider.currentTime()).isNotNull();
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(transactionalCheckoutUseCase).isNotNull();
        assertThat(transactionalCancelOrderUseCase).isNotNull();
        assertThat(applicationContext.getBean(CheckoutUseCase.class)).isSameAs(transactionalCheckoutUseCase);
        assertThat(applicationContext.getBean(CancelOrderUseCase.class)).isSameAs(transactionalCancelOrderUseCase);
        assertThat(getOrderUseCase).isNotNull();
        assertThat(listOrdersUseCase).isNotNull();
        assertThat(getAdminOrderUseCase).isNotNull();
        assertThat(listAdminOrdersUseCase).isNotNull();
        assertThat(updateOrderStatusUseCase).isNotNull();
        assertThat(autoConfirmPendingOrdersUseCase).isNotNull();
        assertThat(applicationContext.getBeanNamesForType(
                        com.superfercho.orders.application.usecase.CheckoutUseCase.class))
                .isEmpty();
        assertThat(applicationContext.getBeanNamesForType(
                        com.superfercho.orders.application.usecase.CancelOrderUseCase.class))
                .isEmpty();
    }
}
