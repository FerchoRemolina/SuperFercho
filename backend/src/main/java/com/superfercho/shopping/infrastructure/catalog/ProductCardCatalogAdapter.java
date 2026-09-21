package com.superfercho.shopping.infrastructure.catalog;

import com.superfercho.catalog.application.port.ProductCardQueryPort;
import com.superfercho.shopping.application.dto.favorite.FavoriteProductView;
import com.superfercho.shopping.application.port.out.ProductCardCatalogPort;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ProductCardCatalogAdapter implements ProductCardCatalogPort {

    private final ProductCardQueryPort productCardQueryPort;

    public ProductCardCatalogAdapter(ProductCardQueryPort productCardQueryPort) {
        this.productCardQueryPort = productCardQueryPort;
    }

    @Override
    public List<FavoriteProductView> findCardsByIds(Collection<UUID> productIds) {
        return productCardQueryPort.findCardsByIds(productIds).stream()
                .map(info -> new FavoriteProductView(
                        info.id(),
                        info.name(),
                        info.brand(),
                        info.price(),
                        info.imageUrl(),
                        info.categoryId(),
                        info.status().name(),
                        info.sellable()))
                .toList();
    }
}
