package com.superfercho.orders.application.port;

import com.superfercho.orders.application.dto.AddressSnapshot;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressPort {

    Optional<AddressSnapshot> getAddressForCustomer(UUID customerId, UUID addressId);
}
