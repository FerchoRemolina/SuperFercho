package com.superfercho.storefront.infrastructure.media;

import com.superfercho.media.application.port.MediaStorage;
import com.superfercho.storefront.application.dto.StoredStorefrontImage;
import com.superfercho.storefront.application.port.StorefrontMediaPort;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto de imágenes del storefront hacia el almacenamiento de
 * Media. La extensión canónica llega detectada por contenido desde la capa
 * REST.
 */
@Component
@Profile("!test")
public class StorefrontMediaAdapter implements StorefrontMediaPort {

    private final MediaStorage mediaStorage;

    public StorefrontMediaAdapter(MediaStorage mediaStorage) {
        this.mediaStorage = Objects.requireNonNull(mediaStorage, "mediaStorage");
    }

    @Override
    public StoredStorefrontImage store(
            byte[] content, String canonicalExtension, String contentType) {
        var stored = mediaStorage.store(
                content,
                toMediaFormat(canonicalExtension, contentType));
        return new StoredStorefrontImage(stored.storageKey(), stored.contentType());
    }

    @Override
    public boolean delete(String storageKey) {
        return mediaStorage.delete(storageKey);
    }

    private static com.superfercho.media.application.MediaImageFormat toMediaFormat(
            String canonicalExtension, String contentType) {
        for (var format : com.superfercho.media.application.MediaImageFormat.values()) {
            if (format.extension().equals(canonicalExtension)
                    && format.contentType().equals(contentType)) {
                return format;
            }
        }
        throw new IllegalArgumentException(
                "formato de imagen no soportado: " + canonicalExtension);
    }
}
