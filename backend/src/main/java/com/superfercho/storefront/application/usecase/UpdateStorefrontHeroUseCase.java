package com.superfercho.storefront.application.usecase;

import com.superfercho.storefront.application.dto.StorefrontHeroResult;
import com.superfercho.storefront.application.dto.UpdateStorefrontHeroCommand;
import com.superfercho.storefront.application.exception.StorefrontContentNotFoundException;
import com.superfercho.storefront.application.port.AdminPrincipalProvider;
import com.superfercho.storefront.application.port.ClockPort;
import com.superfercho.storefront.application.port.StorefrontContentRepository;
import com.superfercho.storefront.domain.model.StorefrontContent;
import java.util.Objects;

/**
 * Actualiza la configuración singleton del Hero. La validación de contenido y
 * topes vive en el dominio; este use case orquesta persistencia, autoría
 * (updatedBy = admin autenticado) y momento de la actualización.
 */
public final class UpdateStorefrontHeroUseCase {

    private final StorefrontContentRepository repository;
    private final AdminPrincipalProvider adminPrincipalProvider;
    private final ClockPort clockPort;

    public UpdateStorefrontHeroUseCase(
            StorefrontContentRepository repository,
            AdminPrincipalProvider adminPrincipalProvider,
            ClockPort clockPort) {
        this.repository = repository;
        this.adminPrincipalProvider = adminPrincipalProvider;
        this.clockPort = clockPort;
    }

    public StorefrontHeroResult execute(UpdateStorefrontHeroCommand command) {
        Objects.requireNonNull(command, "command");
        StorefrontContent existing = repository
                .find()
                .orElseThrow(() -> new StorefrontContentNotFoundException(
                        "La configuración del hero no está inicializada"));
        StorefrontContent updated = existing.update(
                command.title(),
                command.description(),
                command.ctaLabel(),
                command.imageUrl(),
                command.mode(),
                adminPrincipalProvider.currentAdminUserId(),
                clockPort.currentTime());
        return StorefrontHeroResult.from(repository.save(updated, updated.updatedBy(), updated.updatedAt()));
    }
}
