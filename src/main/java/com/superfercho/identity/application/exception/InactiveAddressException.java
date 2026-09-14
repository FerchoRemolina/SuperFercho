package com.superfercho.identity.application.exception;

import java.util.UUID;

public class InactiveAddressException extends RuntimeException {

    public InactiveAddressException(UUID addressId) {
        super("Address is inactive: " + addressId);
    }
}
