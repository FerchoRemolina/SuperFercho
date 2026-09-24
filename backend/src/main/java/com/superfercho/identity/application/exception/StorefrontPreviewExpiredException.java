package com.superfercho.identity.application.exception;

public class StorefrontPreviewExpiredException extends RuntimeException {

    public StorefrontPreviewExpiredException() {
        super("Storefront preview has expired");
    }
}
