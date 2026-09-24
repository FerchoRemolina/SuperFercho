package com.superfercho.orders.infrastructure.identity;

import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.orders.application.port.PreviewCustomerExclusionPort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PreviewCustomerExclusionAdapter implements PreviewCustomerExclusionPort {

    private final CustomerPreviewRepository customerPreviewRepository;

    public PreviewCustomerExclusionAdapter(CustomerPreviewRepository customerPreviewRepository) {
        this.customerPreviewRepository = customerPreviewRepository;
    }

    @Override
    public boolean isPreviewTemporaryCustomer(UUID customerId) {
        return customerPreviewRepository.existsByTemporaryCustomerId(customerId);
    }
}
