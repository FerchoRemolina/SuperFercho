package com.superfercho.orders.infrastructure.persistence.entity;

import com.superfercho.orders.domain.model.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "orders")
public class OrderJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "subtotal_currency", nullable = false, length = 3)
    private String subtotalCurrency;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_currency", nullable = false, length = 3)
    private String totalCurrency;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "shipping_recipient_name", nullable = false)
    private String shippingRecipientName;

    @Column(name = "shipping_address_line", nullable = false)
    private String shippingAddressLine;

    @Column(name = "shipping_additional_info")
    private String shippingAdditionalInfo;

    @Column(name = "shipping_city", nullable = false)
    private String shippingCity;

    @Column(name = "shipping_department", nullable = false)
    private String shippingDepartment;

    @Column(name = "shipping_phone", nullable = false)
    private String shippingPhone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    @OrderColumn(name = "item_index")
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    protected OrderJpaEntity() {
    }

    public OrderJpaEntity(
            UUID id,
            String orderNumber,
            UUID customerId,
            OrderStatus status,
            BigDecimal subtotalAmount,
            String subtotalCurrency,
            BigDecimal totalAmount,
            String totalCurrency,
            UUID paymentId,
            String shippingRecipientName,
            String shippingAddressLine,
            String shippingAdditionalInfo,
            String shippingCity,
            String shippingDepartment,
            String shippingPhone,
            Instant createdAt,
            Instant confirmedAt,
            Instant cancelledAt,
            Instant updatedAt,
            List<OrderItemJpaEntity> items) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.status = status;
        this.subtotalAmount = subtotalAmount;
        this.subtotalCurrency = subtotalCurrency;
        this.totalAmount = totalAmount;
        this.totalCurrency = totalCurrency;
        this.paymentId = paymentId;
        this.shippingRecipientName = shippingRecipientName;
        this.shippingAddressLine = shippingAddressLine;
        this.shippingAdditionalInfo = shippingAdditionalInfo;
        this.shippingCity = shippingCity;
        this.shippingDepartment = shippingDepartment;
        this.shippingPhone = shippingPhone;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
        this.cancelledAt = cancelledAt;
        this.updatedAt = updatedAt;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public UUID getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public String getSubtotalCurrency() {
        return subtotalCurrency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getTotalCurrency() {
        return totalCurrency;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getShippingRecipientName() {
        return shippingRecipientName;
    }

    public String getShippingAddressLine() {
        return shippingAddressLine;
    }

    public String getShippingAdditionalInfo() {
        return shippingAdditionalInfo;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public String getShippingDepartment() {
        return shippingDepartment;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItemJpaEntity> getItems() {
        return items;
    }
}
