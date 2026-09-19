package com.superfercho.orders.infrastructure.configuration;

import com.superfercho.orders.application.dto.CancelOrderCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.port.in.CancelOrderUseCase;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalCancelOrderUseCase implements CancelOrderUseCase {

    private final com.superfercho.orders.application.usecase.CancelOrderUseCase cancelOrderUseCase;
    private final TransactionTemplate transactionTemplate;

    public TransactionalCancelOrderUseCase(
            com.superfercho.orders.application.usecase.CancelOrderUseCase cancelOrderUseCase,
            TransactionTemplate transactionTemplate) {
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public OrderResult execute(CancelOrderCommand command) {
        return transactionTemplate.execute(status -> cancelOrderUseCase.execute(command));
    }
}
