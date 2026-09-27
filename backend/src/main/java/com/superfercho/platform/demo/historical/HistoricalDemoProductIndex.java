package com.superfercho.platform.demo.historical;

import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.platform.demo.LocalDemoDataRunner;
import com.superfercho.platform.demo.LocalDemoDataRunner.DatasetProduct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resolves approved dataset codes (SF-xxx) to persisted catalog products without mutating stock.
 */
public final class HistoricalDemoProductIndex {

    private final Map<String, Product> byCode;

    private HistoricalDemoProductIndex(Map<String, Product> byCode) {
        this.byCode = Map.copyOf(byCode);
    }

    public static HistoricalDemoProductIndex build(
            ProductRepository productRepository, ProductVariantRepository productVariantRepository) {
        List<DatasetProduct> dataset = LocalDemoDataRunner.loadDatasetProducts();
        Map<String, DatasetProduct> datasetByCode = dataset.stream()
                .collect(Collectors.toMap(DatasetProduct::id, product -> product, (left, right) -> left));
        List<Product> products = productRepository.findAll();
        Map<UUID, String> variantNames = productVariantRepository
                .findByIds(products.stream()
                        .map(Product::productVariantId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(ProductVariant::id, ProductVariant::name));

        Map<String, Product> resolved = new HashMap<>();
        for (String code : requiredCodes()) {
            DatasetProduct expected = datasetByCode.get(code);
            if (expected == null) {
                throw new IllegalStateException("Historical demo references unknown dataset product: " + code);
            }
            List<Product> matches = products.stream()
                    .filter(product -> matches(expected, product, variantNames.get(product.productVariantId())))
                    .toList();
            if (matches.size() != 1) {
                throw new IllegalStateException(
                        "Expected exactly one catalog product for " + code + ", found " + matches.size());
            }
            resolved.put(code, matches.getFirst());
        }
        return new HistoricalDemoProductIndex(resolved);
    }

    public Product require(String code) {
        Product product = byCode.get(code);
        if (product == null) {
            throw new IllegalStateException("Historical demo product not indexed: " + code);
        }
        return product;
    }

    public Map<UUID, Integer> snapshotStock() {
        Map<UUID, Integer> stock = new HashMap<>();
        for (Product product : byCode.values()) {
            stock.put(product.id(), product.stock());
        }
        return Map.copyOf(stock);
    }

    static List<String> requiredCodes() {
        return HistoricalDemoBlueprint.customers().stream()
                .flatMap(customer -> java.util.stream.Stream.of(
                                customer.favoriteProductCodes().stream(),
                                customer.cartProductCodes().stream(),
                                customer.shoppingLists().stream().flatMap(list -> list.productCodes().stream()),
                                customer.orders().stream().flatMap(order -> order.productCodes().stream()))
                        .flatMap(stream -> stream))
                .distinct()
                .sorted()
                .toList();
    }

    private static boolean matches(DatasetProduct expected, Product actual, String variantName) {
        if (hasText(expected.barcode())) {
            return expected.barcode().equals(actual.barcode());
        }
        if (!normalize(expected.name()).equals(normalize(actual.name()))) {
            return false;
        }
        if (!normalizeNullable(expected.brand()).equals(normalizeNullable(actual.brand()))) {
            return false;
        }
        if (expected.price().compareTo(actual.price().amount()) != 0) {
            return false;
        }
        if (hasText(expected.description()) || hasText(actual.description())) {
            return normalizeNullable(expected.description()).equals(normalizeNullable(actual.description()));
        }
        if (hasText(expected.variant())) {
            return normalize(expected.variant()).equals(normalizeNullable(variantName));
        }
        return !hasText(variantName);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? "" : normalize(value);
    }

    /** Package-visible for tests verifying BigDecimal equality helpers. */
    static boolean samePrice(BigDecimal expected, BigDecimal actual) {
        return expected.compareTo(actual) == 0;
    }
}
