package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.UserRepository;
import java.util.List;
import java.util.UUID;

public final class ListAddressesUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public ListAddressesUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            AddressRepository addressRepository) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    public List<AddressResult> execute() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        userRepository.findById(currentUserId).orElseThrow(() -> new UserNotFoundException(currentUserId));
        return addressRepository.findByUserId(currentUserId).stream()
                .map(AddressResult::from)
                .toList();
    }
}
