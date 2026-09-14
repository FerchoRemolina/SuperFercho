package com.superfercho.identity.application.dto;

import java.util.UUID;

public record DeactivateAddressCommand(UUID userId, UUID addressId) {
}
