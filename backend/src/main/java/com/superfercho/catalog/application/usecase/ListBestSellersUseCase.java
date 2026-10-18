package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.ProductSales;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.application.port.SalesPort;
import com.superfercho.catalog.domain.model.Product;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * "Los más vendidos" del storefront: ranking por unidades vendidas con
 * ventana móvil y fallback (7 → 30 → 90 días → histórico disponible). Solo
 * avanza de ventana mientras falten productos para completar el límite. Los
 * productos deben ser públicamente visibles y estar disponibles (stock > 0);
 * no se duplican entre ventanas y se aplica una diversidad sencilla por
 * categoría (tope con relleno por ranking) que no destruye el orden de
 * ventas. Si no hay suficientes elegibles se devuelven los que existan.
 */
public final class ListBestSellersUseCase {

    public static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 32;
    static final int CANDIDATE_LIMIT = 48;
    static final int MAX_PER_CATEGORY = 4;
    /** Ventanas móviles antes del histórico disponible (desde época). */
    static final List<Duration> WINDOWS =
            List.of(Duration.ofDays(7), Duration.ofDays(30), Duration.ofDays(90));

    private final SalesPort salesPort;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductVariantRepository productVariantRepository;
    private final Clock clock;

    public ListBestSellersUseCase(
            SalesPort salesPort,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository,
            Clock clock) {
        this.salesPort = salesPort;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productTypeRepository = productTypeRepository;
        this.productVariantRepository = productVariantRepository;
        this.clock = clock;
    }

    public List<ProductResult> execute(Integer limit) {
        int effectiveLimit = limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT);
        List<Product> ranked = rankedEligibleProducts(effectiveLimit);
        List<Product> diversified = applyCategoryDiversity(ranked, MAX_PER_CATEGORY, effectiveLimit);
        return toResults(diversified);
    }

    private List<Product> rankedEligibleProducts(int limit) {
        Instant now = clock.instant();
        Map<UUID, Product> selectedById = new LinkedHashMap<>();
        for (Duration window : WINDOWS) {
            collectEligibleWindow(selectedById, now.minus(window), now, limit);
            if (selectedById.size() >= limit) {
                return List.copyOf(selectedById.values());
            }
        }
        // Último recurso: histórico disponible completo.
        collectEligibleWindow(selectedById, Instant.EPOCH, now, limit);
        return List.copyOf(selectedById.values());
    }

    private void collectEligibleWindow(
            Map<UUID, Product> selectedById, Instant fromInclusive, Instant toExclusive, int limit) {
        List<ProductSales> sales = salesPort.topProductsByQuantity(fromInclusive, toExclusive, CANDIDATE_LIMIT);
        List<UUID> pendingIds = sales.stream()
                .map(ProductSales::productId)
                .filter(productId -> !selectedById.containsKey(productId))
                .toList();
        if (pendingIds.isEmpty()) {
            return;
        }
        Map<UUID, Product> productsById = productRepository.findByIds(pendingIds).stream()
                .collect(Collectors.toMap(Product::id, Function.identity(), (left, right) -> left));
        CatalogVisibilityLookup lookup = CatalogVisibilityLookup.load(
                productsById.values(),
                categoryRepository,
                productTypeRepository,
                productVariantRepository);
        for (UUID productId : pendingIds) {
            if (selectedById.size() >= limit) {
                break;
            }
            Product product = productsById.get(productId);
            if (product == null || product.stock() <= 0 || !lookup.isPubliclyVisible(product)) {
                continue;
            }
            selectedById.put(productId, product);
        }
    }

    /**
     * Primera pasada con tope por categoría respetando el ranking; si quedan
     * cupos, se rellenan con los excedentes en su orden original. No inventa
     * productos ni reordena el ranking.
     */
    static List<Product> applyCategoryDiversity(
            List<Product> ranked, int maxPerCategory, int limit) {
        List<Product> picked = new ArrayList<>();
        List<Product> overflow = new ArrayList<>();
        Map<UUID, Integer> pickedPerCategory = new HashMap<>();
        for (Product product : ranked) {
            if (picked.size() >= limit) {
                break;
            }
            int used = pickedPerCategory.getOrDefault(product.categoryId(), 0);
            if (used >= maxPerCategory) {
                overflow.add(product);
                continue;
            }
            picked.add(product);
            pickedPerCategory.merge(product.categoryId(), 1, Integer::sum);
        }
        for (Product product : overflow) {
            if (picked.size() >= limit) {
                break;
            }
            picked.add(product);
        }
        return picked;
    }

    private List<ProductResult> toResults(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        CatalogVisibilityLookup lookup = CatalogVisibilityLookup.load(
                products, categoryRepository, productTypeRepository, productVariantRepository);
        return products.stream()
                .map(product -> ProductResult.from(
                        product, lookup.productTypeNameOf(product), lookup.productVariantNameOf(product)))
                .toList();
    }
}
