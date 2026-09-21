package com.superfercho.catalog.application.port;

import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findByIds(Collection<UUID> ids);

    List<Product> findAll();

    List<Product> findByCategoryId(UUID categoryId);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategoryIdAndStatus(UUID categoryId, ProductStatus status);

    List<Product> searchByNameBrandOrBarcode(String text);
}
