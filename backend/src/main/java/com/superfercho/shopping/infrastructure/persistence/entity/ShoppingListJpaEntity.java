package com.superfercho.shopping.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shopping_lists", schema = "shopping")
public class ShoppingListJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    @OrderColumn(name = "item_index")
    private List<ShoppingListItemJpaEntity> items = new ArrayList<>();

    protected ShoppingListJpaEntity() {
    }

    public ShoppingListJpaEntity(
            UUID id,
            UUID customerId,
            String name,
            Instant createdAt,
            Instant updatedAt,
            List<ShoppingListItemJpaEntity> items) {
        this.id = id;
        this.customerId = customerId;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ShoppingListItemJpaEntity> getItems() {
        return items;
    }
}
