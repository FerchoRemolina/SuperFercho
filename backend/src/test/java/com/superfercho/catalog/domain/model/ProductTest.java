package com.superfercho.catalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.catalog.domain.exception.InvalidProductException;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProductTest {

    private static final UUID ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID CATEGORY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-01T00:15:00Z");

    @Test
    void shouldCreateValidProduct() {
        Product product = validProduct().build();

        assertEquals(ID, product.id());
        assertEquals(CATEGORY_ID, product.categoryId());
        assertEquals("7701234567890", product.barcode());
        assertEquals("Leche entera", product.name());
        assertEquals("Alpina", product.brand());
        assertEquals("1L whole milk", product.description());
        assertEquals(Money.cop(new BigDecimal("4500.00")), product.price());
        assertEquals(10, product.stock());
        assertEquals("https://cdn.example.com/leche.png", product.imageUrl());
        assertEquals(ProductStatus.ACTIVE, product.status());
        assertEquals(CREATED_AT, product.createdAt());
        assertEquals(UPDATED_AT, product.updatedAt());
    }

    @Test
    void shouldAcceptProductWhenOptionalFieldsAreNull() {
        Product product = validProduct()
                .barcode(null)
                .brand(null)
                .description(null)
                .imageUrl(null)
                .build();

        assertNull(product.barcode());
        assertNull(product.brand());
        assertNull(product.description());
        assertNull(product.imageUrl());
    }

    @Test
    void shouldAcceptProductWhenStockIsZero() {
        Product product = validProduct().stock(0).build();

        assertEquals(0, product.stock());
    }

    @Test
    void shouldRejectProductWhenIdIsNull() {
        assertThrows(InvalidProductException.class, () -> validProduct().id(null).build());
    }

    @Test
    void shouldRejectProductWhenCategoryIdIsNull() {
        assertThrows(InvalidProductException.class, () -> validProduct().categoryId(null).build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectProductWhenNameIsBlank(String blank) {
        assertThrows(InvalidProductException.class, () -> validProduct().name(blank).build());
    }

    @Test
    void shouldRejectProductWhenPriceIsNull() {
        assertThrows(InvalidProductException.class, () -> validProduct().price(null).build());
    }

    @Test
    void shouldRejectProductWhenPriceIsNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validProduct().price(Money.cop(new BigDecimal("-0.01"))).build());
    }

    @Test
    void shouldRejectProductWhenStockIsNegative() {
        assertThrows(InvalidProductException.class, () -> validProduct().stock(-1).build());
    }

    @Test
    void shouldRejectProductWhenStatusIsNull() {
        assertThrows(InvalidProductException.class, () -> validProduct().status(null).build());
    }

    @Test
    void shouldActivateProduct() {
        Product inactive = validProduct().status(ProductStatus.INACTIVE).build();

        Product activated = inactive.activate(LATER);

        assertEquals(ProductStatus.ACTIVE, activated.status());
        assertEquals(inactive.id(), activated.id());
        assertEquals(inactive.categoryId(), activated.categoryId());
        assertEquals(inactive.createdAt(), activated.createdAt());
        assertEquals(LATER, activated.updatedAt());
    }

    @Test
    void shouldDeactivateProduct() {
        Product active = validProduct().build();

        Product deactivated = active.deactivate(LATER);

        assertEquals(ProductStatus.INACTIVE, deactivated.status());
        assertEquals(active.id(), deactivated.id());
        assertEquals(active.stock(), deactivated.stock());
        assertEquals(active.createdAt(), deactivated.createdAt());
        assertEquals(LATER, deactivated.updatedAt());
    }

    @Test
    void shouldUpdateProductInformationAndPreserveIdentity() {
        Product original = validProduct().build();

        Product updated = original.updateInformation(
                original.categoryId(),
                "7709999999999",
                "Leche deslactosada",
                "Alquería",
                "900ml",
                null,
                LATER);

        assertEquals(original.id(), updated.id());
        assertEquals(original.categoryId(), updated.categoryId());
        assertEquals(original.price(), updated.price());
        assertEquals(original.stock(), updated.stock());
        assertEquals(original.status(), updated.status());
        assertEquals(original.createdAt(), updated.createdAt());
        assertEquals("7709999999999", updated.barcode());
        assertEquals("Leche deslactosada", updated.name());
        assertEquals("Alquería", updated.brand());
        assertEquals("900ml", updated.description());
        assertNull(updated.imageUrl());
        assertEquals(LATER, updated.updatedAt());
    }

    @Test
    void shouldUpdateProductCategory() {
        Product original = validProduct().build();
        UUID newCategoryId = UUID.fromString("66666666-6666-6666-6666-666666666666");

        Product updated = original.updateInformation(
                newCategoryId,
                original.barcode(),
                original.name(),
                original.brand(),
                original.description(),
                original.imageUrl(),
                LATER);

        assertEquals(newCategoryId, updated.categoryId());
        assertEquals(original.price(), updated.price());
        assertEquals(original.stock(), updated.stock());
    }

    @Test
    void shouldRejectInformationUpdateWhenNameIsBlank() {
        Product original = validProduct().build();

        assertThrows(
                InvalidProductException.class,
                () -> original.updateInformation(
                        original.categoryId(), "7701234567890", "  ", "Alpina", "1L", null, LATER));
    }

    @Test
    void shouldChangeProductPrice() {
        Product original = validProduct().build();
        Money newPrice = Money.cop(new BigDecimal("5200.00"));

        Product updated = original.changePrice(newPrice, LATER);

        assertEquals(newPrice, updated.price());
        assertEquals(original.id(), updated.id());
        assertEquals(original.stock(), updated.stock());
        assertEquals(original.createdAt(), updated.createdAt());
        assertEquals(LATER, updated.updatedAt());
    }

    @Test
    void shouldRejectPriceChangeWhenPriceIsNull() {
        Product original = validProduct().build();

        assertThrows(InvalidProductException.class, () -> original.changePrice(null, LATER));
    }

    @Test
    void shouldChangeProductStock() {
        Product original = validProduct().stock(10).build();

        Product updated = original.changeStock(3, LATER);

        assertEquals(3, updated.stock());
        assertEquals(original.id(), updated.id());
        assertEquals(original.price(), updated.price());
        assertEquals(original.createdAt(), updated.createdAt());
        assertEquals(LATER, updated.updatedAt());
    }

    @Test
    void shouldRejectStockChangeWhenStockIsNegative() {
        Product original = validProduct().build();

        assertThrows(InvalidProductException.class, () -> original.changeStock(-1, LATER));
    }

    private static ProductBuilder validProduct() {
        return new ProductBuilder();
    }

    private static final class ProductBuilder {
        private UUID id = ID;
        private UUID categoryId = CATEGORY_ID;
        private String barcode = "7701234567890";
        private String name = "Leche entera";
        private String brand = "Alpina";
        private String description = "1L whole milk";
        private Money price = Money.cop(new BigDecimal("4500.00"));
        private int stock = 10;
        private String imageUrl = "https://cdn.example.com/leche.png";
        private ProductStatus status = ProductStatus.ACTIVE;
        private Instant createdAt = CREATED_AT;
        private Instant updatedAt = UPDATED_AT;

        private ProductBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        private ProductBuilder categoryId(UUID categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        private ProductBuilder barcode(String barcode) {
            this.barcode = barcode;
            return this;
        }

        private ProductBuilder name(String name) {
            this.name = name;
            return this;
        }

        private ProductBuilder brand(String brand) {
            this.brand = brand;
            return this;
        }

        private ProductBuilder description(String description) {
            this.description = description;
            return this;
        }

        private ProductBuilder price(Money price) {
            this.price = price;
            return this;
        }

        private ProductBuilder stock(int stock) {
            this.stock = stock;
            return this;
        }

        private ProductBuilder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        private ProductBuilder status(ProductStatus status) {
            this.status = status;
            return this;
        }

        private Product build() {
            return Product.create(
                    id,
                    categoryId,
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
    }
}
