package com.superfercho.catalog.application.port;

import com.superfercho.catalog.application.dto.ProductPriceInfo;
import java.util.Optional;
import java.util.UUID;

public interface ProductQueryPort {

    /**
     * Operational lookup for other modules. Empty when the product does not exist
     * or is not ACTIVE.
     */
    Optional<ProductPriceInfo> findById(UUID productId);
}
