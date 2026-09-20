package com.superfercho.orders.infrastructure.identity;

import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.OwnedAddress;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.orders.application.dto.AddressSnapshot;
import com.superfercho.orders.application.port.CustomerAddressPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CustomerAddressAdapter implements CustomerAddressPort {

    private final AddressRepository addressRepository;

    public CustomerAddressAdapter(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    public Optional<AddressSnapshot> getAddressForCustomer(UUID customerId, UUID addressId) {
        return addressRepository
                .findOwnedById(addressId)
                .filter(owned -> owned.userId().equals(customerId))
                .map(OwnedAddress::address)
                .filter(address -> address.status() == AddressStatus.ACTIVE)
                .map(CustomerAddressAdapter::toSnapshot);
    }

    private static AddressSnapshot toSnapshot(Address address) {
        return new AddressSnapshot(
                address.recipientName(),
                address.addressLine(),
                address.additionalInfo(),
                address.city(),
                address.department(),
                address.phone());
    }
}
