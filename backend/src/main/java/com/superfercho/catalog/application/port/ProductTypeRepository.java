package com.superfercho.catalog.application.port;

import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductTypeRepository {

    ProductType save(ProductType productType);

    Optional<ProductType> findById(UUID id);

    List<ProductType> findByIds(Collection<UUID> ids);

    List<ProductType> findAll();

    List<ProductType> findByCategoryId(UUID categoryId);

    List<ProductType> findByStatus(ProductTypeStatus status);

    List<ProductType> findByCategoryIdAndStatus(UUID categoryId, ProductTypeStatus status);
}
