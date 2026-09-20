package com.superfercho.orders.application.exception;

import java.util.UUID;

public class AddressNotAvailableException extends RuntimeException {

    public AddressNotAvailableException(UUID addressId) {
        super("Address is not available for the current customer: " + addressId);
    }
}
