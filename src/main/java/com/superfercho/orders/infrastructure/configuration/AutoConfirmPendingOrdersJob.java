package com.superfercho.orders.infrastructure.configuration;

import com.superfercho.orders.application.usecase.AutoConfirmPendingOrdersUseCase;
import org.springframework.scheduling.annotation.Scheduled;

public final class AutoConfirmPendingOrdersJob {

    private static final long ONE_MINUTE_MS = 60_000L;

    private final AutoConfirmPendingOrdersUseCase autoConfirmPendingOrdersUseCase;

    public AutoConfirmPendingOrdersJob(AutoConfirmPendingOrdersUseCase autoConfirmPendingOrdersUseCase) {
        this.autoConfirmPendingOrdersUseCase = autoConfirmPendingOrdersUseCase;
    }

    @Scheduled(fixedDelay = ONE_MINUTE_MS, initialDelay = ONE_MINUTE_MS)
    public void execute() {
        autoConfirmPendingOrdersUseCase.execute();
    }
}
