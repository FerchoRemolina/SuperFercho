package com.superfercho.shopping.infrastructure.configuration;

import com.superfercho.shopping.application.dto.cart.IdempotentAddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.IdempotentCartAddResult;
import com.superfercho.shopping.application.exception.DuplicateCartAddOperationException;
import com.superfercho.shopping.application.port.in.ApplyIdempotentCartAddUseCase;
import com.superfercho.shopping.application.service.CartApplicationService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalApplyIdempotentCartAddUseCase implements ApplyIdempotentCartAddUseCase {

    private final CartApplicationService cartApplicationService;
    private final TransactionTemplate transactionTemplate;

    public TransactionalApplyIdempotentCartAddUseCase(
            CartApplicationService cartApplicationService, TransactionTemplate transactionTemplate) {
        this.cartApplicationService = cartApplicationService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public IdempotentCartAddResult execute(IdempotentAddProductToCartCommand command) {
        try {
            return transactionTemplate.execute(status -> cartApplicationService.execute(command));
        } catch (DuplicateCartAddOperationException exception) {
            return transactionTemplate.execute(status -> cartApplicationService.execute(command));
        }
    }

    @Override
    public Optional<IdempotentCartAddResult> replayIfApplied(UUID operationId, int quantity) {
        return cartApplicationService.replayIfApplied(operationId, quantity);
    }
}
