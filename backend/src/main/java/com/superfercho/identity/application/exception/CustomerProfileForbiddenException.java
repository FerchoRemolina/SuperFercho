package com.superfercho.identity.application.exception;

import java.util.UUID;

/** El perfil solo puede consultarse o actualizarse por su CUSTOMER vivo y no-preview. */
public class CustomerProfileForbiddenException extends RuntimeException {

    public CustomerProfileForbiddenException(UUID userId) {
        super("Customer profile can only be viewed or updated by the authenticated live CUSTOMER account: "
                + userId);
    }
}
