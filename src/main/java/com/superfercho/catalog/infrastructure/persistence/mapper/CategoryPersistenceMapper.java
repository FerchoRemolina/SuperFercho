package com.superfercho.catalog.infrastructure.persistence.mapper;

import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryPersistenceMapper {

    public CategoryJpaEntity toEntity(Category category) {
        return new CategoryJpaEntity(
                category.id(),
                category.name(),
                category.description(),
                category.status(),
                category.createdAt(),
                category.updatedAt());
    }

    public Category toDomain(CategoryJpaEntity entity) {
        return Category.create(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
