package com.superfercho.identity.application.exception;

import java.util.UUID;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(UUID addressId) {
        super("Address not found: " + addressId);
    }
}
