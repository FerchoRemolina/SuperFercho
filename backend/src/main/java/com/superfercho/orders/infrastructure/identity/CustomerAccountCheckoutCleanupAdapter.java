package com.superfercho.orders.infrastructure.identity;

import com.superfercho.identity.application.port.CustomerAccountCheckoutCleanupPort;
import com.superfercho.orders.application.port.IdempotencyPort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CustomerAccountCheckoutCleanupAdapter implements CustomerAccountCheckoutCleanupPort {

    private final IdempotencyPort idempotencyPort;

    public CustomerAccountCheckoutCleanupAdapter(IdempotencyPort idempotencyPort) {
        this.idempotencyPort = idempotencyPort;
    }

    @Override
    public void deleteCheckoutIdempotencyForCustomer(UUID customerId) {
        idempotencyPort.deleteAllByCustomerId(customerId);
    }
}
