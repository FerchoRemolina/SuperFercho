package com.superfercho.catalog.infrastructure.persistence;

import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.catalog.infrastructure.persistence.mapper.ProductTypePersistenceMapper;
import com.superfercho.catalog.infrastructure.persistence.repository.ProductTypeJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ProductTypePersistenceAdapter implements ProductTypeRepository {

    private final ProductTypeJpaRepository productTypeJpaRepository;
    private final ProductTypePersistenceMapper productTypePersistenceMapper;

    public ProductTypePersistenceAdapter(
            ProductTypeJpaRepository productTypeJpaRepository,
            ProductTypePersistenceMapper productTypePersistenceMapper) {
        this.productTypeJpaRepository = productTypeJpaRepository;
        this.productTypePersistenceMapper = productTypePersistenceMapper;
    }

    @Override
    public ProductType save(ProductType productType) {
        try {
            return productTypePersistenceMapper.toDomain(
                    productTypeJpaRepository.saveAndFlush(productTypePersistenceMapper.toEntity(productType)));
        } catch (DataIntegrityViolationException exception) {
            throw CatalogConstraintViolationTranslator.translate(exception);
        }
    }

    @Override
    public Optional<ProductType> findById(UUID id) {
        return productTypeJpaRepository.findById(id).map(productTypePersistenceMapper::toDomain);
    }

    @Override
    public List<ProductType> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return productTypeJpaRepository.findAllById(ids).stream()
                .map(productTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProductType> findAll() {
        return productTypeJpaRepository.findAllByOrderByNameIgnoreCaseAsc().stream()
                .map(productTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProductType> findByCategoryId(UUID categoryId) {
        return productTypeJpaRepository.findByCategoryIdOrderByNameIgnoreCaseAsc(categoryId).stream()
                .map(productTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProductType> findByStatus(ProductTypeStatus status) {
        return productTypeJpaRepository.findByStatusOrderByNameIgnoreCaseAsc(status).stream()
                .map(productTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProductType> findByCategoryIdAndStatus(UUID categoryId, ProductTypeStatus status) {
        return productTypeJpaRepository
                .findByCategoryIdAndStatusOrderByNameIgnoreCaseAsc(categoryId, status)
                .stream()
                .map(productTypePersistenceMapper::toDomain)
                .toList();
    }
}
