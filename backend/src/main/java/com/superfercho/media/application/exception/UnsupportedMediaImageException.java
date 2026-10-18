package com.superfercho.media.application.exception;

/**
 * El contenido no corresponde a ningún formato de imagen permitido
 * (PNG, JPEG o WebP) según su firma real.
 */
public class UnsupportedMediaImageException extends RuntimeException {

    public UnsupportedMediaImageException() {
        super("unsupported media image: only PNG, JPEG and WebP are allowed");
    }
}
