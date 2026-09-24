package com.superfercho.catalog.infrastructure.persistence.repository;

import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.catalog.infrastructure.persistence.entity.ProductTypeJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTypeJpaRepository extends JpaRepository<ProductTypeJpaEntity, UUID> {

    List<ProductTypeJpaEntity> findByCategoryId(UUID categoryId);

    List<ProductTypeJpaEntity> findByStatus(ProductTypeStatus status);

    List<ProductTypeJpaEntity> findByCategoryIdAndStatus(UUID categoryId, ProductTypeStatus status);
}
