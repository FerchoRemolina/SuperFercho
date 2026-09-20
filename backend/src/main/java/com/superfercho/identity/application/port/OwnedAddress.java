package com.superfercho.identity.application.port;

import com.superfercho.identity.domain.model.Address;
import java.util.UUID;

public record OwnedAddress(UUID userId, Address address) {
}
