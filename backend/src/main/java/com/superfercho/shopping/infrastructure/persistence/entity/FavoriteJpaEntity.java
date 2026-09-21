package com.superfercho.shopping.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "favorites", schema = "shopping")
public class FavoriteJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FavoriteJpaEntity() {
    }

    public FavoriteJpaEntity(UUID id, UUID customerId, UUID productId, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
