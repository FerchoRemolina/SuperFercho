package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.dto.ListAddressesCommand;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.UserRepository;
import java.util.List;

public final class ListAddressesUseCase {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public ListAddressesUseCase(UserRepository userRepository, AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    public List<AddressResult> execute(ListAddressesCommand command) {
        userRepository
                .findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));
        return addressRepository.findByUserId(command.userId()).stream()
                .map(AddressResult::from)
                .toList();
    }
}
