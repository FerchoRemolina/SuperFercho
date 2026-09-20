package com.superfercho.orders.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.exception.PaymentDeclinedException;
import com.superfercho.orders.application.usecase.CheckoutUseCase;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
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
class TransactionalCheckoutUseCaseTest {

    private static final CheckoutCommand COMMAND = new CheckoutCommand(
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            PaymentMethod.SIMULATED_CARD,
            List.of(),
            "checkout-key-1");
    private static final CheckoutResult RESULT = new CheckoutResult(
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
            "ORD-P-1001",
            OrderStatus.PENDING,
            PaymentStatus.APPROVED,
            Money.cop(new BigDecimal("21.00")));

    @Mock
    private CheckoutUseCase checkoutUseCase;

    @Mock
    private TransactionTemplate transactionTemplate;

    private TransactionalCheckoutUseCase wrapper;

    @BeforeEach
    void setUp() {
        wrapper = new TransactionalCheckoutUseCase(checkoutUseCase, transactionTemplate);
    }

    @Test
    void shouldImplementApplicationCheckoutContract() {
        assertInstanceOf(com.superfercho.orders.application.port.in.CheckoutUseCase.class, wrapper);
    }

    @Test
    void shouldDelegateSameCommandAndReturnSameResultInsideTransaction() {
        stubTransactionExecution();
        when(checkoutUseCase.execute(COMMAND)).thenReturn(RESULT);

        CheckoutResult actual = wrapper.execute(COMMAND);

        assertSame(RESULT, actual);
        verify(checkoutUseCase).execute(COMMAND);
        verify(transactionTemplate).execute(any());
        verifyNoMoreInteractions(checkoutUseCase);
    }

    @Test
    void shouldPropagateDelegateException() {
        stubTransactionExecution();
        when(checkoutUseCase.execute(COMMAND)).thenThrow(new PaymentDeclinedException());

        assertThrows(PaymentDeclinedException.class, () -> wrapper.execute(COMMAND));
        verify(checkoutUseCase).execute(COMMAND);
        verify(transactionTemplate).execute(any());
        verifyNoMoreInteractions(checkoutUseCase);
    }

    @SuppressWarnings("unchecked")
    private void stubTransactionExecution() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<CheckoutResult> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
    }
}
