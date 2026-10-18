package com.superfercho.storefront.application.port;

import com.superfercho.storefront.domain.model.StorefrontFaq;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de las preguntas frecuentes del storefront. */
public interface StorefrontFaqRepository {

    /** Todas las preguntas (admin), ordenadas para visualización. */
    List<StorefrontFaq> findAllOrdered();

    /** Solo las activas (público), ordenadas para visualización. */
    List<StorefrontFaq> findActiveOrdered();

    Optional<StorefrontFaq> findById(UUID id);

    StorefrontFaq save(StorefrontFaq faq);

    void deleteById(UUID id);
}
