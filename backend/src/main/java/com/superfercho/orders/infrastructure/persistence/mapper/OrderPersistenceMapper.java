package com.superfercho.orders.infrastructure.persistence.mapper;

import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderItem;
import com.superfercho.orders.domain.model.OrderNumber;
import com.superfercho.orders.domain.model.ShippingAddressSnapshot;
import com.superfercho.orders.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.superfercho.orders.infrastructure.persistence.entity.OrderJpaEntity;
import com.superfercho.platform.money.Money;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceMapper {

    public OrderJpaEntity toEntity(Order order) {
        ShippingAddressSnapshot address = order.shippingAddress();
        List<OrderItemJpaEntity> items =
                order.items().stream().map(this::toItemEntity).toList();
        return new OrderJpaEntity(
                order.id(),
                order.orderNumber().value(),
                order.customerId(),
                order.status(),
                order.subtotal().amount(),
                order.subtotal().currency(),
                order.total().amount(),
                order.total().currency(),
                order.paymentId(),
                address.recipientName(),
                address.addressLine(),
                address.additionalInfo(),
                address.city(),
                address.department(),
                address.phone(),
                order.createdAt(),
                order.confirmedAt(),
                order.cancelledAt(),
                order.updatedAt(),
                items);
    }

    public Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream().map(this::toDomainItem).toList();
        return Order.reconstitute(
                entity.getId(),
                new OrderNumber(entity.getOrderNumber()),
                entity.getCustomerId(),
                entity.getStatus(),
                items,
                new ShippingAddressSnapshot(
                        entity.getShippingRecipientName(),
                        entity.getShippingAddressLine(),
                        entity.getShippingAdditionalInfo(),
                        entity.getShippingCity(),
                        entity.getShippingDepartment(),
                        entity.getShippingPhone()),
                entity.getPaymentId(),
                entity.getCreatedAt(),
                entity.getConfirmedAt(),
                entity.getCancelledAt(),
                entity.getUpdatedAt());
    }

    private OrderItemJpaEntity toItemEntity(OrderItem item) {
        return new OrderItemJpaEntity(
                item.id(),
                item.productId(),
                item.productName(),
                item.unitPrice().amount(),
                item.unitPrice().currency(),
                item.quantity(),
                item.subtotal().amount(),
                item.subtotal().currency());
    }

    private OrderItem toDomainItem(OrderItemJpaEntity entity) {
        return OrderItem.create(
                entity.getId(),
                entity.getProductId(),
                entity.getProductName(),
                new Money(entity.getUnitPriceAmount(), entity.getUnitPriceCurrency()),
                entity.getQuantity());
    }
}
