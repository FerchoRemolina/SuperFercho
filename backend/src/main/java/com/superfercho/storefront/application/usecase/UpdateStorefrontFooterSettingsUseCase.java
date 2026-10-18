package com.superfercho.storefront.application.usecase;

import com.superfercho.storefront.application.dto.StorefrontFooterSettingsResult;
import com.superfercho.storefront.application.dto.UpdateStorefrontFooterSettingsCommand;
import com.superfercho.storefront.application.port.AdminPrincipalProvider;
import com.superfercho.storefront.application.port.ClockPort;
import com.superfercho.storefront.application.port.StorefrontContentRepository;
import java.util.Objects;
import java.util.UUID;

/** Actualización de los ajustes del footer (ADMIN). */
public final class UpdateStorefrontFooterSettingsUseCase {

    private final StorefrontContentRepository contentRepository;
    private final AdminPrincipalProvider adminPrincipalProvider;
    private final ClockPort clockPort;

    public UpdateStorefrontFooterSettingsUseCase(
            StorefrontContentRepository contentRepository,
            AdminPrincipalProvider adminPrincipalProvider,
            ClockPort clockPort) {
        this.contentRepository =
                Objects.requireNonNull(contentRepository, "contentRepository");
        this.adminPrincipalProvider =
                Objects.requireNonNull(adminPrincipalProvider, "adminPrincipalProvider");
        this.clockPort = Objects.requireNonNull(clockPort, "clockPort");
    }

    public StorefrontFooterSettingsResult execute(UpdateStorefrontFooterSettingsCommand command) {
        Objects.requireNonNull(command, "command");
        var content = contentRepository
                .find()
                .orElseThrow(() -> new com.superfercho.storefront.application.exception.StorefrontContentNotFoundException(
                        "no hay configuración de storefront"));
        UUID adminId = adminPrincipalProvider.currentAdminUserId();
        var updated = content.updateFooterSettings(
                command.supportEmail(),
                command.linkedinUrl(),
                command.githubUrl(),
                adminId,
                clockPort.currentTime());
        contentRepository.save(updated, adminId, clockPort.currentTime());
        return new StorefrontFooterSettingsResult(
                updated.supportEmail(),
                updated.linkedinUrl(),
                updated.githubUrl(),
                updated.qrImageUrl());
    }
}
