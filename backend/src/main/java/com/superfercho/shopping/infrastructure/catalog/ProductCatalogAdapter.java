package com.superfercho.shopping.infrastructure.catalog;

import com.superfercho.catalog.application.port.ProductQueryPort;
import com.superfercho.shopping.application.dto.ProductCatalogInfo;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ProductCatalogAdapter implements ProductCatalogPort {

    private final ProductQueryPort productQueryPort;

    public ProductCatalogAdapter(ProductQueryPort productQueryPort) {
        this.productQueryPort = productQueryPort;
    }

    @Override
    public Optional<ProductCatalogInfo> getProduct(UUID productId) {
        return productQueryPort
                .findById(productId)
                .map(info -> new ProductCatalogInfo(info.productId(), info.currentPrice()));
    }
}
