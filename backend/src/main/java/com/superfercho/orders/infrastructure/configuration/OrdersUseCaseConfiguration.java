package com.superfercho.orders.infrastructure.configuration;

import com.superfercho.catalog.application.port.InventoryPort;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.CurrentUserProvider;
import com.superfercho.orders.application.port.CustomerAddressPort;
import com.superfercho.orders.application.port.CustomerDirectoryPort;
import com.superfercho.orders.application.port.IdempotencyPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.application.port.PreviewCustomerExclusionPort;
import com.superfercho.orders.application.port.ProductCatalogPort;
import com.superfercho.orders.application.port.ShoppingCartPort;
import com.superfercho.orders.application.usecase.AdvanceOrderLifecycleUseCase;
import com.superfercho.orders.application.usecase.CancelAndDeletePreviewOrdersUseCase;
import com.superfercho.orders.application.usecase.CancelOrderUseCase;
import com.superfercho.orders.application.usecase.CheckoutUseCase;
import com.superfercho.orders.application.usecase.GetAdminOrderPeriodSummaryUseCase;
import com.superfercho.orders.application.usecase.GetAdminOrderUseCase;
import com.superfercho.orders.application.usecase.GetAdminSalesPeriodAnalyticsUseCase;
import com.superfercho.orders.application.usecase.GetAdminSalesPeriodSummaryUseCase;
import com.superfercho.orders.application.usecase.GetAdminTopCustomersUseCase;
import com.superfercho.orders.application.usecase.GetAdminTopProductsUseCase;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.application.usecase.ListAdminOrdersUseCase;
import com.superfercho.orders.application.usecase.ListAdminRecentBuyersUseCase;
import com.superfercho.orders.application.usecase.ListOrdersUseCase;
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
            PreviewCustomerExclusionPort previewCustomerExclusionPort,
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
                idempotencyPort,
                previewCustomerExclusionPort);
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
            PreviewCustomerExclusionPort previewCustomerExclusionPort,
            PlatformTransactionManager transactionManager) {
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(
                currentUserProvider,
                ordersClockProvider,
                orderRepository,
                inventoryPort,
                paymentPort,
                previewCustomerExclusionPort);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return new TransactionalCancelOrderUseCase(cancelOrderUseCase, transaction);
    }

    @Bean
    GetOrderUseCase getOrderUseCase(
            CurrentUserProvider currentUserProvider, OrderRepository orderRepository, PaymentPort paymentPort) {
        return new GetOrderUseCase(currentUserProvider, orderRepository, paymentPort);
    }

    @Bean
    ListOrdersUseCase listOrdersUseCase(CurrentUserProvider currentUserProvider, OrderRepository orderRepository) {
        return new ListOrdersUseCase(currentUserProvider, orderRepository);
    }

    @Bean
    GetAdminOrderUseCase getAdminOrderUseCase(
            OrderRepository orderRepository,
            PaymentPort paymentPort,
            CustomerDirectoryPort customerDirectoryPort) {
        return new GetAdminOrderUseCase(orderRepository, paymentPort, customerDirectoryPort);
    }

    @Bean
    ListAdminOrdersUseCase listAdminOrdersUseCase(
            OrderRepository orderRepository, CustomerDirectoryPort customerDirectoryPort) {
        return new ListAdminOrdersUseCase(orderRepository, customerDirectoryPort);
    }

    @Bean
    GetAdminSalesPeriodSummaryUseCase getAdminSalesPeriodSummaryUseCase(
            OrderRepository orderRepository, ClockProvider ordersClockProvider) {
        return new GetAdminSalesPeriodSummaryUseCase(orderRepository, ordersClockProvider);
    }

    @Bean
    GetAdminSalesPeriodAnalyticsUseCase getAdminSalesPeriodAnalyticsUseCase(OrderRepository orderRepository) {
        return new GetAdminSalesPeriodAnalyticsUseCase(orderRepository);
    }

    @Bean
    GetAdminOrderPeriodSummaryUseCase getAdminOrderPeriodSummaryUseCase(OrderRepository orderRepository) {
        return new GetAdminOrderPeriodSummaryUseCase(orderRepository);
    }

    @Bean
    GetAdminTopProductsUseCase getAdminTopProductsUseCase(OrderRepository orderRepository) {
        return new GetAdminTopProductsUseCase(orderRepository);
    }

    @Bean
    GetAdminTopCustomersUseCase getAdminTopCustomersUseCase(OrderRepository orderRepository) {
        return new GetAdminTopCustomersUseCase(orderRepository);
    }

    @Bean
    ListAdminRecentBuyersUseCase listAdminRecentBuyersUseCase(
            OrderRepository orderRepository, ClockProvider ordersClockProvider) {
        return new ListAdminRecentBuyersUseCase(orderRepository, ordersClockProvider);
    }

    @Bean
    AdvanceOrderLifecycleUseCase advanceOrderLifecycleUseCase(
            OrderRepository orderRepository, ClockProvider ordersClockProvider) {
        return new AdvanceOrderLifecycleUseCase(orderRepository, ordersClockProvider);
    }

    @Bean
    CancelAndDeletePreviewOrdersUseCase cancelAndDeletePreviewOrdersUseCase(
            OrderRepository orderRepository, PaymentPort paymentPort, IdempotencyPort idempotencyPort) {
        return new CancelAndDeletePreviewOrdersUseCase(orderRepository, paymentPort, idempotencyPort);
    }
}
