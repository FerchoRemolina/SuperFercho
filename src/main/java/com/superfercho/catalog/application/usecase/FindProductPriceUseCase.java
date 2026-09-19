package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ProductPriceInfo;
import com.superfercho.catalog.application.port.ProductQueryPort;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.ProductStatus;
import java.util.Optional;
import java.util.UUID;

public final class FindProductPriceUseCase implements ProductQueryPort {

    private final ProductRepository productRepository;

    public FindProductPriceUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<ProductPriceInfo> findById(UUID productId) {
        return productRepository
                .findById(productId)
                .filter(product -> product.status() == ProductStatus.ACTIVE)
                .map(product -> new ProductPriceInfo(product.id(), product.price()));
    }
}
