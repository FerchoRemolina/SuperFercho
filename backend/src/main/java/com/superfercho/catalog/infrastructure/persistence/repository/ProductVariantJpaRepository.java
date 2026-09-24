package com.superfercho.catalog.infrastructure.persistence.repository;

import com.superfercho.catalog.domain.model.ProductVariantStatus;
import com.superfercho.catalog.infrastructure.persistence.entity.ProductVariantJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantJpaRepository extends JpaRepository<ProductVariantJpaEntity, UUID> {

    List<ProductVariantJpaEntity> findByProductTypeId(UUID productTypeId);

    List<ProductVariantJpaEntity> findByStatus(ProductVariantStatus status);

    List<ProductVariantJpaEntity> findByProductTypeIdAndStatus(
            UUID productTypeId, ProductVariantStatus status);
}
