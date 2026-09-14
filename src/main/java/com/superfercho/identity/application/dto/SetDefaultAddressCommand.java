package com.superfercho.identity.application.dto;

import java.util.UUID;

public record SetDefaultAddressCommand(UUID userId, UUID addressId) {
}
