package com.superfercho.identity.application.exception;

import java.util.UUID;

public class AddressOwnershipException extends RuntimeException {

    public AddressOwnershipException(UUID userId, UUID addressId) {
        super("Address " + addressId + " does not belong to user " + userId);
    }
}
