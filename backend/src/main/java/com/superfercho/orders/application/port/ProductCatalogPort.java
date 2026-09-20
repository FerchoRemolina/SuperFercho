package com.superfercho.orders.application.port;

import com.superfercho.catalog.application.dto.StockQuantity;
import com.superfercho.orders.application.dto.AvailabilityResult;
import com.superfercho.orders.application.dto.ProductCatalogInfo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCatalogPort {

    Optional<ProductCatalogInfo> getProduct(UUID productId);

    AvailabilityResult checkAvailability(List<StockQuantity> items);
}
