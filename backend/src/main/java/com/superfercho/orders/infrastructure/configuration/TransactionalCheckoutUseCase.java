package com.superfercho.orders.infrastructure.configuration;

import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.port.in.CheckoutUseCase;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalCheckoutUseCase implements CheckoutUseCase {

    private final com.superfercho.orders.application.usecase.CheckoutUseCase checkoutUseCase;
    private final TransactionTemplate transactionTemplate;

    public TransactionalCheckoutUseCase(
            com.superfercho.orders.application.usecase.CheckoutUseCase checkoutUseCase,
            TransactionTemplate transactionTemplate) {
        this.checkoutUseCase = checkoutUseCase;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public CheckoutResult execute(CheckoutCommand command) {
        return transactionTemplate.execute(status -> checkoutUseCase.execute(command));
    }
}
