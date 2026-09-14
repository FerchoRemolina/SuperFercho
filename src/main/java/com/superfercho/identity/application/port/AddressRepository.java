package com.superfercho.identity.application.port;

import com.superfercho.identity.domain.model.Address;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository {

    Address save(UUID userId, Address address);

    Optional<Address> findById(UUID id);
}
