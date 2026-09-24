package com.superfercho.catalog.infrastructure.persistence;

import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
import com.superfercho.catalog.infrastructure.persistence.mapper.ProductVariantPersistenceMapper;
import com.superfercho.catalog.infrastructure.persistence.repository.ProductVariantJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ProductVariantPersistenceAdapter implements ProductVariantRepository {

    private final ProductVariantJpaRepository productVariantJpaRepository;
    private final ProductVariantPersistenceMapper productVariantPersistenceMapper;

    public ProductVariantPersistenceAdapter(
            ProductVariantJpaRepository productVariantJpaRepository,
            ProductVariantPersistenceMapper productVariantPersistenceMapper) {
        this.productVariantJpaRepository = productVariantJpaRepository;
        this.productVariantPersistenceMapper = productVariantPersistenceMapper;
    }

    @Override
    public ProductVariant save(ProductVariant productVariant) {
        try {
            return productVariantPersistenceMapper.toDomain(productVariantJpaRepository.saveAndFlush(
                    productVariantPersistenceMapper.toEntity(productVariant)));
        } catch (DataIntegrityViolationException exception) {
            throw CatalogConstraintViolationTranslator.translate(exception);
        }
    }

    @Override
    public Optional<ProductVariant> findById(UUID id) {
        return productVariantJpaRepository.findById(id).map(productVariantPersistenceMapper::toDomain);
    }

    @Override
    public List<ProductVariant> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return productVariantJpaRepository.findAllById(ids).stream()
                .map(productVariantPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProductVariant> findAll() {
        return productVariantJpaRepository.findAllByOrderByNameIgnoreCaseAsc().stream()
                .map(productVariantPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProductVariant> findByProductTypeId(UUID productTypeId) {
        return productVariantJpaRepository
                .findByProductTypeIdOrderByNameIgnoreCaseAsc(productTypeId)
                .stream()
                .map(productVariantPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProductVariant> findByStatus(ProductVariantStatus status) {
        return productVariantJpaRepository.findByStatusOrderByNameIgnoreCaseAsc(status).stream()
                .map(productVariantPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProductVariant> findByProductTypeIdAndStatus(
            UUID productTypeId, ProductVariantStatus status) {
        return productVariantJpaRepository
                .findByProductTypeIdAndStatusOrderByNameIgnoreCaseAsc(productTypeId, status)
                .stream()
                .map(productVariantPersistenceMapper::toDomain)
                .toList();
    }
}
