package com.superfercho.media.application.port;

import com.superfercho.media.application.MediaImageFormat;
import com.superfercho.media.application.dto.StoredMedia;

/**
 * Almacenamiento de imágenes administradas. La implementación decide dónde
 * persistir el binario (filesystem local hoy; S3/MinIO más adelante); los
 * consumidores solo reciben la storageKey estable generada server-side.
 */
public interface MediaStorage {

    StoredMedia store(byte[] content, MediaImageFormat format);

    /** Elimina el binario por su storageKey. Devuelve true si existía. */
    boolean delete(String storageKey);
}
