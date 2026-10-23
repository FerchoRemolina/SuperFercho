package com.superfercho.shopping.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.cart.IdempotentAddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.IdempotentCartAddResult;
import com.superfercho.shopping.application.exception.DuplicateCartAddOperationException;
import com.superfercho.shopping.application.service.CartApplicationService;
import com.superfercho.shopping.domain.model.CartStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class TransactionalApplyIdempotentCartAddUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-23T18:00:00Z");
    private static final UUID OPERATION_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final IdempotentAddProductToCartCommand COMMAND =
            new IdempotentAddProductToCartCommand(OPERATION_ID, PRODUCT_ID, 2);

    @Mock
    private CartApplicationService cartApplicationService;

    @Mock
    private TransactionTemplate transactionTemplate;

    private TransactionalApplyIdempotentCartAddUseCase wrapper;

    @BeforeEach
    void setUp() {
        wrapper = new TransactionalApplyIdempotentCartAddUseCase(cartApplicationService, transactionTemplate);
    }

    @Test
    void shouldRetryOnceWhenDuplicateOperationRaceOccurs() {
        stubTransactionExecution();
        IdempotentCartAddResult replayed = IdempotentCartAddResult.replayed(cart());
        when(cartApplicationService.execute(COMMAND))
                .thenThrow(new DuplicateCartAddOperationException())
                .thenReturn(replayed);

        IdempotentCartAddResult actual = wrapper.execute(COMMAND);

        assertSame(replayed, actual);
        assertTrue(actual.replayed());
        verify(cartApplicationService, times(2)).execute(COMMAND);
        verify(transactionTemplate, times(2)).execute(any());
    }

    @SuppressWarnings("unchecked")
    private void stubTransactionExecution() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<IdempotentCartAddResult> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
    }

    private static CartResponse cart() {
        return CartResponse.of(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                CUSTOMER_ID,
                CartStatus.ACTIVE,
                List.of(),
                NOW,
                NOW);
    }
}
