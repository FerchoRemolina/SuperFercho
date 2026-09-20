package com.superfercho.catalog.infrastructure.persistence.repository;

import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.infrastructure.persistence.entity.CategoryJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    List<CategoryJpaEntity> findByStatus(CategoryStatus status);

    boolean existsByName(String name);
}
