package com.superfercho.catalog.application.port;

import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository {

    ProductVariant save(ProductVariant productVariant);

    Optional<ProductVariant> findById(UUID id);

    List<ProductVariant> findByIds(Collection<UUID> ids);

    List<ProductVariant> findAll();

    List<ProductVariant> findByProductTypeId(UUID productTypeId);

    List<ProductVariant> findByStatus(ProductVariantStatus status);

    List<ProductVariant> findByProductTypeIdAndStatus(UUID productTypeId, ProductVariantStatus status);
}
