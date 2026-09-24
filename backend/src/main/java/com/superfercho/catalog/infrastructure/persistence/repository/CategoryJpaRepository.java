package com.superfercho.catalog.infrastructure.persistence.repository;

import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.infrastructure.persistence.entity.CategoryJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.status = :status ORDER BY LOWER(c.name) ASC")
    List<CategoryJpaEntity> findByStatusOrderByNameIgnoreCaseAsc(@Param("status") CategoryStatus status);

    @Query("SELECT c FROM CategoryJpaEntity c ORDER BY LOWER(c.name) ASC")
    List<CategoryJpaEntity> findAllByOrderByNameIgnoreCaseAsc();

    boolean existsByName(String name);
}
