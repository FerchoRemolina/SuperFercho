package com.superfercho.media.application.exception;

/** El contenido excede el tamaño máximo permitido (2 MiB). */
public class MediaTooLargeException extends RuntimeException {

    public MediaTooLargeException() {
        super("media image exceeds the maximum allowed size of 2 MiB");
    }
}
