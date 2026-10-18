package com.superfercho.orders.infrastructure.sales;

import com.superfercho.catalog.application.dto.ProductSales;
import com.superfercho.catalog.application.port.SalesPort;
import com.superfercho.orders.application.port.OrderRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Read model de ventas para el catálogo (best sellers). Reutiliza la
 * agregación existente de órdenes; los pedidos CANCELLED no contribuyen.
 */
@Component("ordersSalesAdapter")
@Profile("!test")
public class OrderSalesAdapter implements SalesPort {

    private final OrderRepository orderRepository;

    public OrderSalesAdapter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<ProductSales> topProductsByQuantity(
            Instant fromInclusive, Instant toExclusive, int limit) {
        return orderRepository
                .findTopProductsByQuantity(fromInclusive, toExclusive, limit, false)
                .stream()
                .map(row -> new ProductSales(row.productId(), row.quantity()))
                .toList();
    }
}
