package com.superfercho.catalog.infrastructure.persistence;

import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.infrastructure.persistence.mapper.ProductPersistenceMapper;
import com.superfercho.catalog.infrastructure.persistence.repository.ProductJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ProductPersistenceAdapter implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;
    private final ProductPersistenceMapper productPersistenceMapper;

    public ProductPersistenceAdapter(
            ProductJpaRepository productJpaRepository, ProductPersistenceMapper productPersistenceMapper) {
        this.productJpaRepository = productJpaRepository;
        this.productPersistenceMapper = productPersistenceMapper;
    }

    @Override
    public Product save(Product product) {
        try {
            return productPersistenceMapper.toDomain(
                    productJpaRepository.saveAndFlush(productPersistenceMapper.toEntity(product)));
        } catch (DataIntegrityViolationException exception) {
            throw CatalogConstraintViolationTranslator.translate(exception);
        }
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return productJpaRepository.findById(id).map(productPersistenceMapper::toDomain);
    }

    @Override
    public List<Product> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return productJpaRepository.findAllById(ids).stream()
                .map(productPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findAll() {
        return productJpaRepository.findAll().stream()
                .map(productPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findByCategoryId(UUID categoryId) {
        return productJpaRepository.findByCategoryId(categoryId).stream()
                .map(productPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findByStatus(ProductStatus status) {
        return productJpaRepository.findByStatus(status).stream()
                .map(productPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findByCategoryIdAndStatus(UUID categoryId, ProductStatus status) {
        return productJpaRepository.findByCategoryIdAndStatus(categoryId, status).stream()
                .map(productPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> searchByNameBrandOrBarcode(String text) {
        return productJpaRepository.searchByNameBrandOrBarcode(text).stream()
                .map(productPersistenceMapper::toDomain)
                .toList();
    }
}
