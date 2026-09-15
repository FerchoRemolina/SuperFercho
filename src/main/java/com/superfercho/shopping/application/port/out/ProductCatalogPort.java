package com.superfercho.shopping.application.port.out;

import com.superfercho.shopping.application.dto.ProductCatalogInfo;
import java.util.Optional;
import java.util.UUID;

public interface ProductCatalogPort {

    Optional<ProductCatalogInfo> getProduct(UUID productId);
}
