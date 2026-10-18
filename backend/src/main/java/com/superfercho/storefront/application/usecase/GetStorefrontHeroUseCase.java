package com.superfercho.storefront.application.usecase;

import com.superfercho.storefront.application.dto.StorefrontHeroResult;
import com.superfercho.storefront.application.exception.StorefrontContentNotFoundException;
import com.superfercho.storefront.application.port.StorefrontContentRepository;
import com.superfercho.storefront.domain.model.StorefrontContent;
import java.util.Objects;

/** Lectura pública de la configuración del Hero. */
public final class GetStorefrontHeroUseCase {

    private final StorefrontContentRepository repository;

    public GetStorefrontHeroUseCase(StorefrontContentRepository repository) {
        this.repository = repository;
    }

    public StorefrontHeroResult execute() {
        Objects.requireNonNull(repository, "repository");
        StorefrontContent content =
                repository.find().orElseThrow(() -> new StorefrontContentNotFoundException(
                        "La configuración del hero no está inicializada"));
        return StorefrontHeroResult.from(content);
    }
}
