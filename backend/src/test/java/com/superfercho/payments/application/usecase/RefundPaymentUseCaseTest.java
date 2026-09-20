package com.superfercho.payments.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.payments.application.dto.PaymentResponse;
import com.superfercho.payments.application.dto.RefundPaymentCommand;
import com.superfercho.payments.application.exception.PaymentNotFoundException;
import com.superfercho.payments.application.port.ClockPort;
import com.superfercho.payments.application.port.PaymentRepository;
import com.superfercho.payments.domain.exception.InvalidPaymentException;
import com.superfercho.payments.domain.model.Payment;
import com.superfercho.payments.domain.model.PaymentMethod;
import com.superfercho.payments.domain.model.PaymentStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundPaymentUseCaseTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-05-01T10:05:00Z");
    private static final UUID PAYMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Money AMOUNT = Money.cop(new BigDecimal("21.00"));

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ClockPort clockPort;

    private RefundPaymentUseCase refundPayment;

    @BeforeEach
    void setUp() {
        refundPayment = new RefundPaymentUseCase(paymentRepository, clockPort);
    }

    @Test
    void shouldRefundApprovedPayment() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(approvedPayment()));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = refundPayment.execute(new RefundPaymentCommand(PAYMENT_ID));

        assertEquals(PAYMENT_ID, response.paymentId());
        assertEquals(PaymentStatus.APPROVED, response.status());
        assertEquals(NOW, response.refundedAt());
        assertEquals(NOW, response.updatedAt());
        assertEquals(CREATED_AT, response.createdAt());

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).findById(PAYMENT_ID);
        verify(clockPort).currentTime();
        verify(paymentRepository).save(saved.capture());
        assertEquals(NOW, saved.getValue().refundedAt());
        assertEquals(PaymentStatus.APPROVED, saved.getValue().status());
    }

    @Test
    void shouldRejectWhenPaymentDoesNotExist() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThrows(
                PaymentNotFoundException.class, () -> refundPayment.execute(new RefundPaymentCommand(PAYMENT_ID)));
        verify(paymentRepository).findById(PAYMENT_ID);
        verify(clockPort, never()).currentTime();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldPropagateDomainRejectionAndNotSave() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment()));
        when(clockPort.currentTime()).thenReturn(NOW);

        assertThrows(
                InvalidPaymentException.class, () -> refundPayment.execute(new RefundPaymentCommand(PAYMENT_ID)));
        verify(paymentRepository).findById(PAYMENT_ID);
        verify(clockPort).currentTime();
        verify(paymentRepository, never()).save(any());
    }

    private static Payment approvedPayment() {
        return Payment.create(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.SIMULATED_CARD,
                PaymentStatus.APPROVED,
                "sim-approved",
                CREATED_AT);
    }

    private static Payment pendingPayment() {
        return Payment.create(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.CASH_ON_DELIVERY,
                PaymentStatus.PENDING,
                "cod-pending",
                CREATED_AT);
    }
}
