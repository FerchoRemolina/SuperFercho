package com.superfercho.catalog.infrastructure.persistence.repository;

import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.catalog.infrastructure.persistence.entity.ProductTypeJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductTypeJpaRepository extends JpaRepository<ProductTypeJpaEntity, UUID> {

    @Query(
            "SELECT t FROM ProductTypeJpaEntity t WHERE t.categoryId = :categoryId ORDER BY LOWER(t.name) ASC")
    List<ProductTypeJpaEntity> findByCategoryIdOrderByNameIgnoreCaseAsc(
            @Param("categoryId") UUID categoryId);

    @Query("SELECT t FROM ProductTypeJpaEntity t WHERE t.status = :status ORDER BY LOWER(t.name) ASC")
    List<ProductTypeJpaEntity> findByStatusOrderByNameIgnoreCaseAsc(
            @Param("status") ProductTypeStatus status);

    @Query(
            "SELECT t FROM ProductTypeJpaEntity t WHERE t.categoryId = :categoryId AND t.status = :status"
                    + " ORDER BY LOWER(t.name) ASC")
    List<ProductTypeJpaEntity> findByCategoryIdAndStatusOrderByNameIgnoreCaseAsc(
            @Param("categoryId") UUID categoryId, @Param("status") ProductTypeStatus status);

    @Query("SELECT t FROM ProductTypeJpaEntity t ORDER BY LOWER(t.name) ASC")
    List<ProductTypeJpaEntity> findAllByOrderByNameIgnoreCaseAsc();
}
