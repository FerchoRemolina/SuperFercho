package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.cart.IdempotentAddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.IdempotentCartAddResult;
import java.util.Optional;
import java.util.UUID;

public interface ApplyIdempotentCartAddUseCase {

    IdempotentCartAddResult execute(IdempotentAddProductToCartCommand command);

    /**
     * Returns the current cart when {@code operationId} already has a receipt for the
     * authenticated customer and {@code quantity}. Does not add. Empty when no receipt
     * exists or it belongs to another customer.
     */
    Optional<IdempotentCartAddResult> replayIfApplied(UUID operationId, int quantity);
}
