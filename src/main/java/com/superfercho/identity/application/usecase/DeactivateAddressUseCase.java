package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.dto.DeactivateAddressCommand;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.domain.model.Address;
import java.time.Clock;

public final class DeactivateAddressUseCase {

    private final AddressRepository addressRepository;
    private final Clock clock;

    public DeactivateAddressUseCase(AddressRepository addressRepository, Clock clock) {
        this.addressRepository = addressRepository;
        this.clock = clock;
    }

    public AddressResult execute(DeactivateAddressCommand command) {
        Address address = OwnedAddressAccess.requireOwnedAddress(
                addressRepository, command.userId(), command.addressId());
        Address deactivated = address.deactivate(clock.instant());
        return AddressResult.from(addressRepository.save(command.userId(), deactivated));
    }
}
