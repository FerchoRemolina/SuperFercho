package com.superfercho.catalog.infrastructure.persistence.entity;

import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.ProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "catalog")
public class ProductJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "product_type_id", nullable = false)
    private UUID productTypeId;

    @Column(name = "product_variant_id")
    private UUID productVariantId;

    @Column(name = "presentation_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal presentationQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "presentation_unit", nullable = false)
    private PresentationUnit presentationUnit;

    @Column(name = "barcode")
    private String barcode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "brand")
    private String brand;

    @Column(name = "description")
    private String description;

    @Column(name = "price_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductJpaEntity() {
    }

    public ProductJpaEntity(
            UUID id,
            UUID categoryId,
            UUID productTypeId,
            UUID productVariantId,
            BigDecimal presentationQuantity,
            PresentationUnit presentationUnit,
            String barcode,
            String name,
            String brand,
            String description,
            BigDecimal priceAmount,
            String currency,
            int stock,
            String imageUrl,
            ProductStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.productTypeId = productTypeId;
        this.productVariantId = productVariantId;
        this.presentationQuantity = presentationQuantity;
        this.presentationUnit = presentationUnit;
        this.barcode = barcode;
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.priceAmount = priceAmount;
        this.currency = currency;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public UUID getProductTypeId() {
        return productTypeId;
    }

    public UUID getProductVariantId() {
        return productVariantId;
    }

    public BigDecimal getPresentationQuantity() {
        return presentationQuantity;
    }

    public PresentationUnit getPresentationUnit() {
        return presentationUnit;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public int getStock() {
        return stock;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
