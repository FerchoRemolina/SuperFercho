package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.dto.SetDefaultAddressCommand;
import com.superfercho.identity.application.exception.InactiveAddressException;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class SetDefaultAddressUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final AddressRepository addressRepository;
    private final Clock clock;

    public SetDefaultAddressUseCase(
            CurrentUserProvider currentUserProvider, AddressRepository addressRepository, Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.addressRepository = addressRepository;
        this.clock = clock;
    }

    public AddressResult execute(SetDefaultAddressCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Address address = OwnedAddressAccess.requireOwnedAddress(
                addressRepository, currentUserId, command.addressId());
        if (address.status() != AddressStatus.ACTIVE) {
            throw new InactiveAddressException(address.id());
        }

        Instant now = clock.instant();
        addressRepository
                .findActiveDefaultByUserId(currentUserId)
                .filter(currentDefault -> !currentDefault.id().equals(address.id()))
                .ifPresent(currentDefault ->
                        addressRepository.save(currentUserId, currentDefault.clearDefault(now)));

        Address markedDefault = address.markAsDefault(now);
        return AddressResult.from(addressRepository.save(currentUserId, markedDefault));
    }
}
