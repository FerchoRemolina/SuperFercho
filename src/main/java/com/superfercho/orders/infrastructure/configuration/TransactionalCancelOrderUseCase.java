package com.superfercho.orders.infrastructure.configuration;

import com.superfercho.orders.application.dto.CancelOrderCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.usecase.CancelOrderUseCase;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalCancelOrderUseCase {

    private final CancelOrderUseCase cancelOrderUseCase;
    private final TransactionTemplate transactionTemplate;

    public TransactionalCancelOrderUseCase(
            CancelOrderUseCase cancelOrderUseCase, TransactionTemplate transactionTemplate) {
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.transactionTemplate = transactionTemplate;
    }

    public OrderResult execute(CancelOrderCommand command) {
        return transactionTemplate.execute(status -> cancelOrderUseCase.execute(command));
    }
}
