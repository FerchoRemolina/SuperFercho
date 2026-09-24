package com.superfercho.catalog.infrastructure.persistence.repository;

import com.superfercho.catalog.domain.model.ProductVariantStatus;
import com.superfercho.catalog.infrastructure.persistence.entity.ProductVariantJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantJpaRepository extends JpaRepository<ProductVariantJpaEntity, UUID> {

    @Query(
            "SELECT v FROM ProductVariantJpaEntity v WHERE v.productTypeId = :productTypeId"
                    + " ORDER BY LOWER(v.name) ASC")
    List<ProductVariantJpaEntity> findByProductTypeIdOrderByNameIgnoreCaseAsc(
            @Param("productTypeId") UUID productTypeId);

    @Query(
            "SELECT v FROM ProductVariantJpaEntity v WHERE v.status = :status ORDER BY LOWER(v.name) ASC")
    List<ProductVariantJpaEntity> findByStatusOrderByNameIgnoreCaseAsc(
            @Param("status") ProductVariantStatus status);

    @Query(
            "SELECT v FROM ProductVariantJpaEntity v WHERE v.productTypeId = :productTypeId"
                    + " AND v.status = :status ORDER BY LOWER(v.name) ASC")
    List<ProductVariantJpaEntity> findByProductTypeIdAndStatusOrderByNameIgnoreCaseAsc(
            @Param("productTypeId") UUID productTypeId,
            @Param("status") ProductVariantStatus status);

    @Query("SELECT v FROM ProductVariantJpaEntity v ORDER BY LOWER(v.name) ASC")
    List<ProductVariantJpaEntity> findAllByOrderByNameIgnoreCaseAsc();
}
