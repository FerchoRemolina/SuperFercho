package com.superfercho.catalog.domain.model;

import com.superfercho.catalog.domain.exception.InvalidProductException;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

public final class Product {

    private final UUID id;
    private final UUID categoryId;
    private final UUID productTypeId;
    private final UUID productVariantId;
    private final Presentation presentation;
    private final String barcode;
    private final String name;
    private final String brand;
    private final String description;
    private final Money price;
    private final int stock;
    private final String imageUrl;
    private final ProductStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Product(
            UUID id,
            UUID categoryId,
            UUID productTypeId,
            UUID productVariantId,
            Presentation presentation,
            String barcode,
            String name,
            String brand,
            String description,
            Money price,
            int stock,
            String imageUrl,
            ProductStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.productTypeId = productTypeId;
        this.productVariantId = productVariantId;
        this.presentation = presentation;
        this.barcode = barcode;
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product create(
            UUID id,
            UUID categoryId,
            UUID productTypeId,
            UUID productVariantId,
            Presentation presentation,
            String barcode,
            String name,
            String brand,
            String description,
            Money price,
            int stock,
            String imageUrl,
            ProductStatus status,
            Instant createdAt,
            Instant updatedAt) {
        requireNonNull(id, "id");
        requireNonNull(categoryId, "categoryId");
        requireNonNull(productTypeId, "productTypeId");
        requireNonNull(presentation, "presentation");
        requireText(name, "name");
        requireNonNull(price, "price");
        requireNonNull(status, "status");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(updatedAt, "updatedAt");
        if (stock < 0) {
            throw new InvalidProductException("stock cannot be negative");
        }
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidProductException("createdAt must not be after updatedAt");
        }

        return new Product(
                id,
                categoryId,
                productTypeId,
                productVariantId,
                presentation,
                barcode,
                name,
                brand,
                description,
                price,
                stock,
                imageUrl,
                status,
                createdAt,
                updatedAt);
    }

    public Product updateInformation(
            UUID categoryId,
            UUID productTypeId,
            UUID productVariantId,
            Presentation presentation,
            String barcode,
            String name,
            String brand,
            String description,
            String imageUrl,
            Instant updatedAt) {
        return create(
                id,
                categoryId,
                productTypeId,
                productVariantId,
                presentation,
                barcode,
                name,
                brand,
                description,
                price,
                stock,
                imageUrl,
                status,
                createdAt,
                updatedAt);
    }

    public Product activate(Instant updatedAt) {
        if (status != ProductStatus.INACTIVE) {
            throw new InvalidProductException("product can only be activated when INACTIVE");
        }
        return withStatus(ProductStatus.ACTIVE, updatedAt);
    }

    public Product deactivate(Instant updatedAt) {
        if (status != ProductStatus.ACTIVE) {
            throw new InvalidProductException("product can only be deactivated when ACTIVE");
        }
        return withStatus(ProductStatus.INACTIVE, updatedAt);
    }

    public Product archive(Instant updatedAt) {
        if (status == ProductStatus.ARCHIVED) {
            throw new InvalidProductException("product is already ARCHIVED");
        }
        return withStatus(ProductStatus.ARCHIVED, updatedAt);
    }

    public Product restore(Instant updatedAt) {
        if (status != ProductStatus.ARCHIVED) {
            throw new InvalidProductException("product can only be restored when ARCHIVED");
        }
        return withStatus(ProductStatus.INACTIVE, updatedAt);
    }

    public Product changePrice(Money price, Instant updatedAt) {
        return create(
                id,
                categoryId,
                productTypeId,
                productVariantId,
                presentation,
                barcode,
                name,
                brand,
                description,
                price,
                stock,
                imageUrl,
                status,
                createdAt,
                updatedAt);
    }

    public Product changeStock(int stock, Instant updatedAt) {
        return create(
                id,
                categoryId,
                productTypeId,
                productVariantId,
                presentation,
                barcode,
                name,
                brand,
                description,
                price,
                stock,
                imageUrl,
                status,
                createdAt,
                updatedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID categoryId() {
        return categoryId;
    }

    public UUID productTypeId() {
        return productTypeId;
    }

    public UUID productVariantId() {
        return productVariantId;
    }

    public Presentation presentation() {
        return presentation;
    }

    public String barcode() {
        return barcode;
    }

    public String name() {
        return name;
    }

    public String brand() {
        return brand;
    }

    public String description() {
        return description;
    }

    public Money price() {
        return price;
    }

    public int stock() {
        return stock;
    }

    public String imageUrl() {
        return imageUrl;
    }

    public ProductStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private Product withStatus(ProductStatus newStatus, Instant updatedAt) {
        return create(
                id,
                categoryId,
                productTypeId,
                productVariantId,
                presentation,
                barcode,
                name,
                brand,
                description,
                price,
                stock,
                imageUrl,
                newStatus,
                createdAt,
                updatedAt);
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidProductException(field + " cannot be null");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidProductException(field + " cannot be null or blank");
        }
    }
}
