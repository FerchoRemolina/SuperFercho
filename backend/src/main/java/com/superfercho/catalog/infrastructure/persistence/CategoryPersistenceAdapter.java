package com.superfercho.catalog.infrastructure.persistence;

import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.infrastructure.persistence.mapper.CategoryPersistenceMapper;
import com.superfercho.catalog.infrastructure.persistence.repository.CategoryJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CategoryPersistenceAdapter implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;
    private final CategoryPersistenceMapper categoryPersistenceMapper;

    public CategoryPersistenceAdapter(
            CategoryJpaRepository categoryJpaRepository,
            CategoryPersistenceMapper categoryPersistenceMapper) {
        this.categoryJpaRepository = categoryJpaRepository;
        this.categoryPersistenceMapper = categoryPersistenceMapper;
    }

    @Override
    public Category save(Category category) {
        try {
            return categoryPersistenceMapper.toDomain(
                    categoryJpaRepository.saveAndFlush(categoryPersistenceMapper.toEntity(category)));
        } catch (DataIntegrityViolationException exception) {
            throw CatalogConstraintViolationTranslator.translate(exception);
        }
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return categoryJpaRepository.findById(id).map(categoryPersistenceMapper::toDomain);
    }

    @Override
    public List<Category> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return categoryJpaRepository.findAllById(ids).stream()
                .map(categoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Category> findAll() {
        return categoryJpaRepository.findAllByOrderByNameIgnoreCaseAsc().stream()
                .map(categoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Category> findByStatus(CategoryStatus status) {
        return categoryJpaRepository.findByStatusOrderByNameIgnoreCaseAsc(status).stream()
                .map(categoryPersistenceMapper::toDomain)
                .toList();
    }
}
