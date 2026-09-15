package com.superfercho.shopping.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items", schema = "shopping")
public class CartItemJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price_at_addition_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtAdditionAmount;

    @Column(name = "price_at_addition_currency", nullable = false, length = 3)
    private String priceAtAdditionCurrency;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CartItemJpaEntity() {
    }

    public CartItemJpaEntity(
            UUID id,
            UUID productId,
            int quantity,
            BigDecimal priceAtAdditionAmount,
            String priceAtAdditionCurrency,
            Instant addedAt,
            Instant updatedAt) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtAdditionAmount = priceAtAdditionAmount;
        this.priceAtAdditionCurrency = priceAtAdditionCurrency;
        this.addedAt = addedAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPriceAtAdditionAmount() {
        return priceAtAdditionAmount;
    }

    public String getPriceAtAdditionCurrency() {
        return priceAtAdditionCurrency;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
