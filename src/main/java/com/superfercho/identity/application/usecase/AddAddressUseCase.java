package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AddAddressCommand;
import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class AddAddressUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final Clock clock;

    public AddAddressUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            AddressRepository addressRepository,
            Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.clock = clock;
    }

    public AddressResult execute(AddAddressCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        userRepository.findById(currentUserId).orElseThrow(() -> new UserNotFoundException(currentUserId));

        Instant now = clock.instant();
        Address address = Address.create(
                UUID.randomUUID(),
                command.label(),
                command.recipientName(),
                command.addressLine(),
                command.additionalInfo(),
                command.city(),
                command.department(),
                command.phone(),
                command.isDefault(),
                AddressStatus.ACTIVE,
                now,
                now);

        if (address.isDefault()) {
            clearCurrentDefault(currentUserId, now);
        }

        return AddressResult.from(addressRepository.save(currentUserId, address));
    }

    private void clearCurrentDefault(UUID userId, Instant now) {
        addressRepository
                .findActiveDefaultByUserId(userId)
                .ifPresent(currentDefault ->
                        addressRepository.save(userId, currentDefault.clearDefault(now)));
    }
}
