package com.superfercho.payments.infrastructure.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentRequest;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.payments.application.dto.GetPaymentCommand;
import com.superfercho.payments.application.dto.PaymentResponse;
import com.superfercho.payments.application.dto.ProcessPaymentCommand;
import com.superfercho.payments.application.dto.RefundPaymentCommand;
import com.superfercho.payments.application.exception.PaymentNotFoundException;
import com.superfercho.payments.application.usecase.GetPaymentUseCase;
import com.superfercho.payments.application.usecase.ProcessPaymentUseCase;
import com.superfercho.payments.application.usecase.RefundPaymentUseCase;
import com.superfercho.payments.domain.exception.InvalidPaymentException;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentIntegrationAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");
    private static final UUID PAYMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Money AMOUNT = Money.cop(new BigDecimal("21.00"));

    @Mock
    private ProcessPaymentUseCase processPaymentUseCase;

    @Mock
    private GetPaymentUseCase getPaymentUseCase;

    @Mock
    private RefundPaymentUseCase refundPaymentUseCase;

    private PaymentIntegrationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PaymentIntegrationAdapter(processPaymentUseCase, getPaymentUseCase, refundPaymentUseCase);
    }

    @Test
    void shouldProcessSimulatedCardPayment() {
        when(processPaymentUseCase.execute(any(ProcessPaymentCommand.class))).thenReturn(cardResponse());

        PaymentResult result =
                adapter.processPayment(new PaymentRequest(ORDER_ID, AMOUNT, PaymentMethod.SIMULATED_CARD));

        ArgumentCaptor<ProcessPaymentCommand> command = ArgumentCaptor.forClass(ProcessPaymentCommand.class);
        verify(processPaymentUseCase).execute(command.capture());
        assertEquals(ORDER_ID, command.getValue().orderId());
        assertEquals(AMOUNT, command.getValue().amount());
        assertEquals(
                com.superfercho.payments.domain.model.PaymentMethod.SIMULATED_CARD,
                command.getValue().paymentMethod());
        assertEquals(PAYMENT_ID, result.paymentId());
        assertEquals(PaymentStatus.APPROVED, result.status());
        assertEquals("sim-approved", result.providerReference());
        verifyNoInteractions(getPaymentUseCase, refundPaymentUseCase);
    }

    @Test
    void shouldProcessCashOnDeliveryPayment() {
        when(processPaymentUseCase.execute(any(ProcessPaymentCommand.class))).thenReturn(codResponse());

        PaymentResult result =
                adapter.processPayment(new PaymentRequest(ORDER_ID, AMOUNT, PaymentMethod.CASH_ON_DELIVERY));

        ArgumentCaptor<ProcessPaymentCommand> command = ArgumentCaptor.forClass(ProcessPaymentCommand.class);
        verify(processPaymentUseCase).execute(command.capture());
        assertEquals(ORDER_ID, command.getValue().orderId());
        assertEquals(AMOUNT, command.getValue().amount());
        assertEquals(
                com.superfercho.payments.domain.model.PaymentMethod.CASH_ON_DELIVERY,
                command.getValue().paymentMethod());
        assertEquals(PAYMENT_ID, result.paymentId());
        assertEquals(PaymentStatus.PENDING, result.status());
        assertEquals("cod-pending", result.providerReference());
        verifyNoInteractions(getPaymentUseCase, refundPaymentUseCase);
    }

    @Test
    void shouldGetPayment() {
        when(getPaymentUseCase.execute(any(GetPaymentCommand.class))).thenReturn(cardResponse());

        PaymentResult result = adapter.getPayment(PAYMENT_ID);

        ArgumentCaptor<GetPaymentCommand> command = ArgumentCaptor.forClass(GetPaymentCommand.class);
        verify(getPaymentUseCase).execute(command.capture());
        assertEquals(PAYMENT_ID, command.getValue().paymentId());
        assertEquals(PAYMENT_ID, result.paymentId());
        assertEquals(AMOUNT, result.amount());
        assertEquals(PaymentStatus.APPROVED, result.status());
        assertEquals("sim-approved", result.providerReference());
        verifyNoInteractions(processPaymentUseCase, refundPaymentUseCase);
    }

    @Test
    void shouldMapDeclinedStatusFromGetPayment() {
        when(getPaymentUseCase.execute(any(GetPaymentCommand.class))).thenReturn(declinedResponse());

        PaymentResult result = adapter.getPayment(PAYMENT_ID);

        assertEquals(PAYMENT_ID, result.paymentId());
        assertEquals(PaymentStatus.DECLINED, result.status());
        assertEquals("sim-declined", result.providerReference());
    }

    @Test
    void shouldRefundPaymentWithoutInventingAResult() {
        when(refundPaymentUseCase.execute(any(RefundPaymentCommand.class))).thenReturn(refundedResponse());

        adapter.refundPayment(PAYMENT_ID);

        ArgumentCaptor<RefundPaymentCommand> command = ArgumentCaptor.forClass(RefundPaymentCommand.class);
        verify(refundPaymentUseCase).execute(command.capture());
        assertEquals(PAYMENT_ID, command.getValue().paymentId());
        verify(processPaymentUseCase, never()).execute(any());
        verify(getPaymentUseCase, never()).execute(any());
    }

    @Test
    void shouldPropagatePaymentNotFoundFromGetPayment() {
        PaymentNotFoundException missing = new PaymentNotFoundException(PAYMENT_ID);
        when(getPaymentUseCase.execute(any(GetPaymentCommand.class))).thenThrow(missing);

        PaymentNotFoundException thrown =
                assertThrows(PaymentNotFoundException.class, () -> adapter.getPayment(PAYMENT_ID));
        assertSame(missing, thrown);
    }

    @Test
    void shouldPropagatePaymentNotFoundFromRefund() {
        PaymentNotFoundException missing = new PaymentNotFoundException(PAYMENT_ID);
        when(refundPaymentUseCase.execute(any(RefundPaymentCommand.class))).thenThrow(missing);

        PaymentNotFoundException thrown =
                assertThrows(PaymentNotFoundException.class, () -> adapter.refundPayment(PAYMENT_ID));
        assertSame(missing, thrown);
    }

    @Test
    void shouldPropagateInvalidPaymentFromRefund() {
        InvalidPaymentException invalid = new InvalidPaymentException("Payment is not approved");
        when(refundPaymentUseCase.execute(any(RefundPaymentCommand.class))).thenThrow(invalid);

        InvalidPaymentException thrown =
                assertThrows(InvalidPaymentException.class, () -> adapter.refundPayment(PAYMENT_ID));
        assertSame(invalid, thrown);
    }

    private static PaymentResponse cardResponse() {
        return new PaymentResponse(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                com.superfercho.payments.domain.model.PaymentMethod.SIMULATED_CARD,
                com.superfercho.payments.domain.model.PaymentStatus.APPROVED,
                "sim-approved",
                NOW,
                NOW,
                null);
    }

    private static PaymentResponse codResponse() {
        return new PaymentResponse(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                com.superfercho.payments.domain.model.PaymentMethod.CASH_ON_DELIVERY,
                com.superfercho.payments.domain.model.PaymentStatus.PENDING,
                "cod-pending",
                NOW,
                NOW,
                null);
    }

    private static PaymentResponse declinedResponse() {
        return new PaymentResponse(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                com.superfercho.payments.domain.model.PaymentMethod.SIMULATED_CARD,
                com.superfercho.payments.domain.model.PaymentStatus.DECLINED,
                "sim-declined",
                NOW,
                NOW,
                null);
    }

    private static PaymentResponse refundedResponse() {
        return new PaymentResponse(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                com.superfercho.payments.domain.model.PaymentMethod.SIMULATED_CARD,
                com.superfercho.payments.domain.model.PaymentStatus.APPROVED,
                "sim-approved",
                NOW,
                NOW,
                NOW);
    }
}
