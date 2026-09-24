package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.domain.model.CustomerPreview;
import java.time.Clock;
import java.util.List;

public final class ExpireStorefrontPreviewsUseCase {

    private final CustomerPreviewRepository customerPreviewRepository;
    private final FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase;
    private final Clock clock;

    public ExpireStorefrontPreviewsUseCase(
            CustomerPreviewRepository customerPreviewRepository,
            FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase,
            Clock clock) {
        this.customerPreviewRepository = customerPreviewRepository;
        this.finalizeStorefrontPreviewUseCase = finalizeStorefrontPreviewUseCase;
        this.clock = clock;
    }

    public int execute() {
        List<CustomerPreview> expired = customerPreviewRepository.findExpiredActive(clock.instant());
        int closed = 0;
        for (CustomerPreview preview : expired) {
            if (finalizeStorefrontPreviewUseCase.execute(preview.id())) {
                closed++;
            }
        }
        return closed;
    }
}
