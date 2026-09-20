package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.dto.DeactivateAddressCommand;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.domain.model.Address;
import java.time.Clock;
import java.util.UUID;

public final class DeactivateAddressUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final AddressRepository addressRepository;
    private final Clock clock;

    public DeactivateAddressUseCase(
            CurrentUserProvider currentUserProvider, AddressRepository addressRepository, Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.addressRepository = addressRepository;
        this.clock = clock;
    }

    public AddressResult execute(DeactivateAddressCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Address address = OwnedAddressAccess.requireOwnedAddress(
                addressRepository, currentUserId, command.addressId());
        Address deactivated = address.deactivate(clock.instant());
        return AddressResult.from(addressRepository.save(currentUserId, deactivated));
    }
}
