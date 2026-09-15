package com.superfercho.orders.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.CancelOrderCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.usecase.CancelOrderUseCase;
import com.superfercho.orders.domain.exception.OrderCancellationNotAllowedException;
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
class TransactionalCancelOrderUseCaseTest {

    private static final CancelOrderCommand COMMAND =
            new CancelOrderCommand(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

    @Mock
    private CancelOrderUseCase cancelOrderUseCase;

    @Mock
    private TransactionTemplate transactionTemplate;

    private TransactionalCancelOrderUseCase wrapper;
    private OrderResult result;

    @BeforeEach
    void setUp() {
        wrapper = new TransactionalCancelOrderUseCase(cancelOrderUseCase, transactionTemplate);
        result = mock(OrderResult.class);
    }

    @Test
    void shouldDelegateSameCommandAndReturnSameResultInsideTransaction() {
        stubTransactionExecution();
        when(cancelOrderUseCase.execute(COMMAND)).thenReturn(result);

        OrderResult actual = wrapper.execute(COMMAND);

        assertSame(result, actual);
        verify(cancelOrderUseCase).execute(COMMAND);
        verify(transactionTemplate).execute(any());
        verifyNoMoreInteractions(cancelOrderUseCase);
    }

    @Test
    void shouldPropagateDelegateException() {
        stubTransactionExecution();
        when(cancelOrderUseCase.execute(COMMAND)).thenThrow(new OrderCancellationNotAllowedException("window expired"));

        assertThrows(OrderCancellationNotAllowedException.class, () -> wrapper.execute(COMMAND));
        verify(cancelOrderUseCase).execute(COMMAND);
        verify(transactionTemplate).execute(any());
        verifyNoMoreInteractions(cancelOrderUseCase);
    }

    @SuppressWarnings("unchecked")
    private void stubTransactionExecution() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<OrderResult> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
    }
}
