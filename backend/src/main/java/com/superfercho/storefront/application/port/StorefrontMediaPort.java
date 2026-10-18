package com.superfercho.storefront.application.port;

import com.superfercho.storefront.application.dto.StoredStorefrontImage;

/** Puerto propio del storefront hacia el almacenamiento de imágenes de Media. */
public interface StorefrontMediaPort {

    StoredStorefrontImage store(byte[] content, String canonicalExtension, String contentType);

    /** Elimina el binario por su storageKey. Devuelve true si existía. */
    boolean delete(String storageKey);
}
