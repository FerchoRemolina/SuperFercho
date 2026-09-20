package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.exception.AddressNotFoundException;
import com.superfercho.identity.application.exception.AddressOwnershipException;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.OwnedAddress;
import com.superfercho.identity.domain.model.Address;
import java.util.UUID;

final class OwnedAddressAccess {

    private OwnedAddressAccess() {
    }

    static Address requireOwnedAddress(AddressRepository addresses, UUID userId, UUID addressId) {
        OwnedAddress owned = addresses
                .findOwnedById(addressId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));
        if (!owned.userId().equals(userId)) {
            throw new AddressOwnershipException(userId, addressId);
        }
        return owned.address();
    }
}
