package com.superfercho.media.application;

/**
 * Formatos de imagen aceptados por Media. La extensión y el content type
 * canónicos se derivan siempre del formato detectado por contenido; nunca del
 * filename ni del Content-Type enviado por el cliente.
 */
public enum MediaImageFormat {
    PNG(".png", "image/png"),
    JPEG(".jpg", "image/jpeg"),
    WEBP(".webp", "image/webp");

    private final String extension;
    private final String contentType;

    MediaImageFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }
}
