package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.dto.UpdateAddressCommand;
import com.superfercho.identity.application.exception.InactiveAddressException;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import java.time.Clock;

public final class UpdateAddressUseCase {

    private final AddressRepository addressRepository;
    private final Clock clock;

    public UpdateAddressUseCase(AddressRepository addressRepository, Clock clock) {
        this.addressRepository = addressRepository;
        this.clock = clock;
    }

    public AddressResult execute(UpdateAddressCommand command) {
        Address address = OwnedAddressAccess.requireOwnedAddress(
                addressRepository, command.userId(), command.addressId());
        if (address.status() != AddressStatus.ACTIVE) {
            throw new InactiveAddressException(address.id());
        }

        Address updated = address.updateDetails(
                command.label(),
                command.recipientName(),
                command.addressLine(),
                command.additionalInfo(),
                command.city(),
                command.department(),
                command.phone(),
                clock.instant());

        return AddressResult.from(addressRepository.save(command.userId(), updated));
    }
}
