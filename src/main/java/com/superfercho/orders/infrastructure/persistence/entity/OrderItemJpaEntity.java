package com.superfercho.orders.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items", schema = "orders")
public class OrderItemJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceAmount;

    @Column(name = "unit_price_currency", nullable = false, length = 3)
    private String unitPriceCurrency;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "subtotal_currency", nullable = false, length = 3)
    private String subtotalCurrency;

    protected OrderItemJpaEntity() {
    }

    public OrderItemJpaEntity(
            UUID id,
            UUID productId,
            String productName,
            BigDecimal unitPriceAmount,
            String unitPriceCurrency,
            int quantity,
            BigDecimal subtotalAmount,
            String subtotalCurrency) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.unitPriceAmount = unitPriceAmount;
        this.unitPriceCurrency = unitPriceCurrency;
        this.quantity = quantity;
        this.subtotalAmount = subtotalAmount;
        this.subtotalCurrency = subtotalCurrency;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPriceAmount() {
        return unitPriceAmount;
    }

    public String getUnitPriceCurrency() {
        return unitPriceCurrency;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public String getSubtotalCurrency() {
        return subtotalCurrency;
    }
}
