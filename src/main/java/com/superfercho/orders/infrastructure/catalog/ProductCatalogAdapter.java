package com.superfercho.orders.infrastructure.catalog;

import com.superfercho.catalog.application.dto.StockQuantity;
import com.superfercho.catalog.application.port.ProductQueryPort;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.orders.application.dto.AvailabilityResult;
import com.superfercho.orders.application.dto.ProductCatalogInfo;
import com.superfercho.orders.application.port.ProductCatalogPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("ordersProductCatalogAdapter")
@Profile("!test")
public class ProductCatalogAdapter implements ProductCatalogPort {

    private final ProductRepository productRepository;
    private final ProductQueryPort productQueryPort;

    public ProductCatalogAdapter(ProductRepository productRepository, ProductQueryPort productQueryPort) {
        this.productRepository = productRepository;
        this.productQueryPort = productQueryPort;
    }

    @Override
    public Optional<ProductCatalogInfo> getProduct(UUID productId) {
        return productRepository.findById(productId).map(product -> toInfo(product, isSellable(product.id())));
    }

    @Override
    public AvailabilityResult checkAvailability(List<StockQuantity> items) {
        List<UUID> unavailableProductIds = new ArrayList<>();
        for (StockQuantity item : items) {
            Optional<Product> product = productRepository.findById(item.productId());
            if (product.isEmpty() || product.get().stock() < item.quantity()) {
                unavailableProductIds.add(item.productId());
            }
        }
        if (unavailableProductIds.isEmpty()) {
            return AvailabilityResult.available();
        }
        return AvailabilityResult.unavailable(unavailableProductIds);
    }

    private boolean isSellable(UUID productId) {
        return productQueryPort.findById(productId).isPresent();
    }

    private static ProductCatalogInfo toInfo(Product product, boolean sellable) {
        return new ProductCatalogInfo(
                product.id(),
                product.name(),
                product.price(),
                product.stock() > 0,
                sellable);
    }
}
