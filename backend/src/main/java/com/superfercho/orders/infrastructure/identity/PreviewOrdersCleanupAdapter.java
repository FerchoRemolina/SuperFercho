package com.superfercho.orders.infrastructure.identity;

import com.superfercho.identity.application.port.PreviewOrdersCleanupPort;
import com.superfercho.orders.application.usecase.CancelAndDeletePreviewOrdersUseCase;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PreviewOrdersCleanupAdapter implements PreviewOrdersCleanupPort {

    private final CancelAndDeletePreviewOrdersUseCase cancelAndDeletePreviewOrdersUseCase;

    public PreviewOrdersCleanupAdapter(CancelAndDeletePreviewOrdersUseCase cancelAndDeletePreviewOrdersUseCase) {
        this.cancelAndDeletePreviewOrdersUseCase = cancelAndDeletePreviewOrdersUseCase;
    }

    @Override
    public void cancelAndDeleteAllForCustomer(UUID customerId) {
        cancelAndDeletePreviewOrdersUseCase.execute(customerId);
    }
}
