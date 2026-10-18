package com.superfercho.storefront.application.port;

import com.superfercho.storefront.domain.model.StorefrontContent;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de la configuración singleton del Hero. */
public interface StorefrontContentRepository {

    Optional<StorefrontContent> find();

    StorefrontContent save(StorefrontContent content, UUID updatedBy, java.time.Instant updatedAt);
}
