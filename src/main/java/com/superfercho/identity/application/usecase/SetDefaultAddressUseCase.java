package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.dto.SetDefaultAddressCommand;
import com.superfercho.identity.application.exception.InactiveAddressException;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import java.time.Clock;
import java.time.Instant;

public class SetDefaultAddressUseCase {

    private final AddressRepository addressRepository;
    private final Clock clock;

    public SetDefaultAddressUseCase(AddressRepository addressRepository, Clock clock) {
        this.addressRepository = addressRepository;
        this.clock = clock;
    }

    public AddressResult execute(SetDefaultAddressCommand command) {
        Address address = OwnedAddressAccess.requireOwnedAddress(
                addressRepository, command.userId(), command.addressId());
        if (address.status() != AddressStatus.ACTIVE) {
            throw new InactiveAddressException(address.id());
        }

        Instant now = clock.instant();
        addressRepository
                .findActiveDefaultByUserId(command.userId())
                .filter(currentDefault -> !currentDefault.id().equals(address.id()))
                .ifPresent(currentDefault ->
                        addressRepository.save(command.userId(), currentDefault.clearDefault(now)));

        Address markedDefault = address.markAsDefault(now);
        return AddressResult.from(addressRepository.save(command.userId(), markedDefault));
    }
}
