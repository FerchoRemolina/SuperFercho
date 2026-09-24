package com.superfercho.identity.application.fakes;

import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.OwnedAddress;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryAddressRepository implements AddressRepository {

    private final Map<UUID, OwnedAddress> addresses = new LinkedHashMap<>();

    @Override
    public Address save(UUID userId, Address address) {
        addresses.put(address.id(), new OwnedAddress(userId, address));
        return address;
    }

    @Override
    public Optional<Address> findById(UUID id) {
        return Optional.ofNullable(addresses.get(id)).map(OwnedAddress::address);
    }

    @Override
    public Optional<OwnedAddress> findOwnedById(UUID id) {
        return Optional.ofNullable(addresses.get(id));
    }

    @Override
    public Optional<Address> findActiveDefaultByUserId(UUID userId) {
        return addresses.values().stream()
                .filter(owned -> owned.userId().equals(userId))
                .map(OwnedAddress::address)
                .filter(address -> address.isDefault() && address.status() == AddressStatus.ACTIVE)
                .findFirst();
    }

    @Override
    public List<Address> findByUserId(UUID userId) {
        return addresses.values().stream()
                .filter(owned -> owned.userId().equals(userId))
                .map(OwnedAddress::address)
                .toList();
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        addresses.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));
    }
}
