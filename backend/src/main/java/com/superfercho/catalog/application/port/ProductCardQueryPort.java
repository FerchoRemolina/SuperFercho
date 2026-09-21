package com.superfercho.catalog.application.port;

import com.superfercho.catalog.application.dto.ProductCardInfo;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductCardQueryPort {

    /**
     * Batch card projection for other modules. Returns every matching product,
     * including INACTIVE ones. {@code sellable} is true only when the product and
     * its category are both ACTIVE. Missing ids are omitted.
     */
    List<ProductCardInfo> findCardsByIds(Collection<UUID> productIds);
}
