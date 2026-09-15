package com.superfercho.orders.infrastructure.configuration;

import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.usecase.CheckoutUseCase;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalCheckoutUseCase {

    private final CheckoutUseCase checkoutUseCase;
    private final TransactionTemplate transactionTemplate;

    public TransactionalCheckoutUseCase(CheckoutUseCase checkoutUseCase, TransactionTemplate transactionTemplate) {
        this.checkoutUseCase = checkoutUseCase;
        this.transactionTemplate = transactionTemplate;
    }

    public CheckoutResult execute(CheckoutCommand command) {
        return transactionTemplate.execute(status -> checkoutUseCase.execute(command));
    }
}
