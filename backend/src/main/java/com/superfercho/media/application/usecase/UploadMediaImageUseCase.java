package com.superfercho.media.application.usecase;

import com.superfercho.media.application.MediaImageFormat;
import com.superfercho.media.application.dto.StoredMedia;
import com.superfercho.media.application.exception.MediaTooLargeException;
import com.superfercho.media.application.exception.UnsupportedMediaImageException;
import com.superfercho.media.application.port.MediaStorage;
import java.util.Objects;

/**
 * Reglas de negocio para la subida de una imagen administrada: tamaño máximo
 * 2 MiB, validado aquí (independiente de los límites globales de Spring), y
 * delegación del almacenamiento con el formato ya detectado por contenido.
 */
public final class UploadMediaImageUseCase {

    public static final long MAX_IMAGE_BYTES = 2L * 1024L * 1024L;

    private final MediaStorage mediaStorage;

    public UploadMediaImageUseCase(MediaStorage mediaStorage) {
        this.mediaStorage = Objects.requireNonNull(mediaStorage, "mediaStorage");
    }

    public StoredMedia upload(byte[] content, MediaImageFormat format) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(format, "format");
        if (content.length == 0) {
            throw new UnsupportedMediaImageException();
        }
        if (content.length > MAX_IMAGE_BYTES) {
            throw new MediaTooLargeException();
        }
        return mediaStorage.store(content, format);
    }
}
