package com.superfercho.orders.application.port;

import com.superfercho.orders.application.dto.CartSnapshot;
import java.util.UUID;

public interface ShoppingCartPort {

    CartSnapshot getActiveCart(UUID customerId);

    void clearCart(UUID customerId);
}
