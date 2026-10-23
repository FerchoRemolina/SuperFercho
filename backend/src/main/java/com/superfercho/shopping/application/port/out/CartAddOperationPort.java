package com.superfercho.shopping.application.port.out;

import com.superfercho.shopping.application.dto.cart.CartAddOperation;
import java.util.Optional;
import java.util.UUID;

public interface CartAddOperationPort {

    Optional<CartAddOperation> find(UUID operationId);

    Optional<CartAddOperation> findAndLock(UUID operationId);

    void save(CartAddOperation operation);

    void deleteAllByCustomerId(UUID customerId);
}
