package com.superfercho.orders.infrastructure.configuration;

import com.superfercho.catalog.application.port.InventoryPort;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.CurrentUserProvider;
import com.superfercho.orders.application.port.CustomerAddressPort;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.application.port.ProductCatalogPort;
import com.superfercho.orders.application.port.ShoppingCartPort;
import com.superfercho.orders.application.usecase.AutoConfirmPendingOrdersUseCase;
import com.superfercho.orders.application.usecase.CancelOrderUseCase;
import com.superfercho.orders.application.usecase.CheckoutUseCase;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.application.usecase.ListOrdersUseCase;
import com.superfercho.orders.application.usecase.UpdateOrderStatusUseCase;
import com.superfercho.orders.infrastructure.clock.SystemClockAdapter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Profile("!test")
public class OrdersUseCaseConfiguration {

    @Bean
    ClockProvider ordersClockProvider(Clock clock) {
        return new SystemClockAdapter(clock);
    }

    @Bean
    TransactionalCheckoutUseCase transactionalCheckoutUseCase(
            CurrentUserProvider currentUserProvider,
            ClockProvider ordersClockProvider,
            ShoppingCartPort shoppingCartPort,
            CustomerAddressPort customerAddressPort,
            ProductCatalogPort productCatalogPort,
            InventoryPort inventoryPort,
            PaymentPort paymentPort,
            OrderRepository orderRepository,
            IdempotencyPort idempotencyPort,
            PlatformTransactionManager transactionManager) {
        CheckoutUseCase checkoutUseCase = new CheckoutUseCase(
                currentUserProvider,
                ordersClockProvider,
                shoppingCartPort,
                customerAddressPort,
                productCatalogPort,
                inventoryPort,
                paymentPort,
                orderRepository,
                idempotencyPort);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return new TransactionalCheckoutUseCase(checkoutUseCase, transaction);
    }

    @Bean
    TransactionalCancelOrderUseCase transactionalCancelOrderUseCase(
            CurrentUserProvider currentUserProvider,
            ClockProvider ordersClockProvider,
            OrderRepository orderRepository,
            InventoryPort inventoryPort,
            PaymentPort paymentPort,
            PlatformTransactionManager transactionManager) {
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(
                currentUserProvider, ordersClockProvider, orderRepository, inventoryPort, paymentPort);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return new TransactionalCancelOrderUseCase(cancelOrderUseCase, transaction);
    }

    @Bean
    GetOrderUseCase getOrderUseCase(CurrentUserProvider currentUserProvider, OrderRepository orderRepository) {
        return new GetOrderUseCase(currentUserProvider, orderRepository);
    }

    @Bean
    ListOrdersUseCase listOrdersUseCase(CurrentUserProvider currentUserProvider, OrderRepository orderRepository) {
        return new ListOrdersUseCase(currentUserProvider, orderRepository);
    }

    @Bean
    UpdateOrderStatusUseCase updateOrderStatusUseCase(
            OrderRepository orderRepository, ClockProvider ordersClockProvider) {
        return new UpdateOrderStatusUseCase(orderRepository, ordersClockProvider);
    }

    @Bean
    AutoConfirmPendingOrdersUseCase autoConfirmPendingOrdersUseCase(
            OrderRepository orderRepository, ClockProvider ordersClockProvider) {
        return new AutoConfirmPendingOrdersUseCase(orderRepository, ordersClockProvider);
    }
}
