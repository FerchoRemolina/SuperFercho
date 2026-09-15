package com.superfercho.shopping.application.port.out;

import com.superfercho.shopping.domain.model.Cart;
import java.util.Optional;
import java.util.UUID;

public interface CartRepositoryPort {

    Cart save(Cart cart);

    Optional<Cart> findByCustomerId(UUID customerId);
}
