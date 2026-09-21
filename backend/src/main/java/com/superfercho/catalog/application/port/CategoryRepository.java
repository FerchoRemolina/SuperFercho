package com.superfercho.catalog.application.port;

import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(UUID id);

    List<Category> findByIds(Collection<UUID> ids);

    List<Category> findAll();

    List<Category> findByStatus(CategoryStatus status);
}
