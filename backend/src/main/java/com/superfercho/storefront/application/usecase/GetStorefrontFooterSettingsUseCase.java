package com.superfercho.storefront.application.usecase;

import com.superfercho.storefront.application.dto.StorefrontFooterSettingsResult;
import com.superfercho.storefront.application.port.StorefrontContentRepository;
import java.util.Objects;

/** Lectura pública de los ajustes del footer (correo, redes y QR). */
public final class GetStorefrontFooterSettingsUseCase {

    private final StorefrontContentRepository contentRepository;

    public GetStorefrontFooterSettingsUseCase(StorefrontContentRepository contentRepository) {
        this.contentRepository = Objects.requireNonNull(contentRepository, "contentRepository");
    }

    public StorefrontFooterSettingsResult execute() {
        var content = contentRepository
                .find()
                .orElseThrow(() -> new com.superfercho.storefront.application.exception.StorefrontContentNotFoundException(
                        "no hay configuración de storefront"));
        return new StorefrontFooterSettingsResult(
                content.supportEmail(),
                content.linkedinUrl(),
                content.githubUrl(),
                content.qrImageUrl());
    }
}
