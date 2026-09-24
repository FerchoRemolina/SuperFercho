package com.superfercho.identity.application.exception;

public class StorefrontPreviewNotFoundException extends RuntimeException {

    public StorefrontPreviewNotFoundException() {
        super("No active storefront preview found");
    }
}
