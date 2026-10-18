package com.superfercho.storefront.application.usecase;

import com.superfercho.storefront.application.dto.StoredStorefrontImage;
import com.superfercho.storefront.application.dto.StorefrontQrImageResult;
import com.superfercho.storefront.application.port.AdminPrincipalProvider;
import com.superfercho.storefront.application.port.ClockPort;
import com.superfercho.storefront.application.port.StorefrontContentRepository;
import com.superfercho.storefront.application.port.StorefrontMediaPort;
import java.util.Objects;
import java.util.UUID;

/**
 * Carga y reemplazo de la imagen del QR (ADMIN). Almacena el binario mediante
 * el puerto de Media (formato detectado por contenido en la capa REST),
 * persiste la URL pública de referencia y limpia el archivo reemplazado cuando
 * ningún otro recurso del storefront lo referencia.
 */
public final class UpdateStorefrontQrImageUseCase {

    public static final String PUBLIC_IMAGES_BASE_URL = "/api/v1/media/images";
    private static final String MEDIA_URL_PREFIX = PUBLIC_IMAGES_BASE_URL + "/";

    private final StorefrontContentRepository contentRepository;
    private final StorefrontMediaPort storefrontMediaPort;
    private final AdminPrincipalProvider adminPrincipalProvider;
    private final ClockPort clockPort;

    public UpdateStorefrontQrImageUseCase(
            StorefrontContentRepository contentRepository,
            StorefrontMediaPort storefrontMediaPort,
            AdminPrincipalProvider adminPrincipalProvider,
            ClockPort clockPort) {
        this.contentRepository =
                Objects.requireNonNull(contentRepository, "contentRepository");
        this.storefrontMediaPort =
                Objects.requireNonNull(storefrontMediaPort, "storefrontMediaPort");
        this.adminPrincipalProvider =
                Objects.requireNonNull(adminPrincipalProvider, "adminPrincipalProvider");
        this.clockPort = Objects.requireNonNull(clockPort, "clockPort");
    }

    public StorefrontQrImageResult execute(
            byte[] content, String canonicalExtension, String contentType) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(canonicalExtension, "canonicalExtension");
        var current = contentRepository
                .find()
                .orElseThrow(() -> new com.superfercho.storefront.application.exception.StorefrontContentNotFoundException(
                        "no hay configuración de storefront"));
        UUID adminId = adminPrincipalProvider.currentAdminUserId();

        StoredStorefrontImage stored =
                storefrontMediaPort.store(content, canonicalExtension, contentType);
        String publicUrl = MEDIA_URL_PREFIX + stored.storageKey();
        String previousKey = storageKeyOf(current.qrImageUrl());

        contentRepository.save(
                current.updateQrImageUrl(publicUrl, adminId, clockPort.currentTime()),
                adminId,
                clockPort.currentTime());

        if (previousKey != null && !previousKey.equals(stored.storageKey())
                && !referencedByHero(current.heroImageUrl(), previousKey)) {
            storefrontMediaPort.delete(previousKey);
        }
        return new StorefrontQrImageResult(publicUrl);
    }

    /** Elimina la referencia al QR y el archivo, si no está referenciado por el hero. */
    public StorefrontQrImageResult removeImage() {
        var current = contentRepository
                .find()
                .orElseThrow(() -> new com.superfercho.storefront.application.exception.StorefrontContentNotFoundException(
                        "no hay configuración de storefront"));
        UUID adminId = adminPrincipalProvider.currentAdminUserId();
        String previousKey = storageKeyOf(current.qrImageUrl());
        contentRepository.save(
                current.updateQrImageUrl(null, adminId, clockPort.currentTime()),
                adminId,
                clockPort.currentTime());
        if (previousKey != null && !referencedByHero(current.heroImageUrl(), previousKey)) {
            storefrontMediaPort.delete(previousKey);
        }
        return new StorefrontQrImageResult(null);
    }

    private static String storageKeyOf(String qrImageUrl) {
        if (qrImageUrl == null || !qrImageUrl.startsWith(MEDIA_URL_PREFIX)) {
            return null;
        }
        return qrImageUrl.substring(MEDIA_URL_PREFIX.length());
    }

    private static boolean referencedByHero(String heroImageUrl, String storageKey) {
        return heroImageUrl != null && heroImageUrl.contains("/media/images/" + storageKey);
    }
}
