package com.superfercho.catalog.application.port;

import com.superfercho.catalog.application.dto.ProductPriceInfo;
import java.util.Optional;
import java.util.UUID;

public interface ProductQueryPort {

    Optional<ProductPriceInfo> findById(UUID productId);
}
