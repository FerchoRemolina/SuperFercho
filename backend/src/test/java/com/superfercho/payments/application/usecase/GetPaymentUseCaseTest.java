package com.superfercho.payments.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.payments.application.dto.GetPaymentCommand;
import com.superfercho.payments.application.dto.PaymentResponse;
import com.superfercho.payments.application.exception.PaymentNotFoundException;
import com.superfercho.payments.application.port.PaymentRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPaymentUseCaseTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-01T10:00:00Z");
    private static final UUID PAYMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Money AMOUNT = Money.cop(new BigDecimal("21.00"));

    @Mock
    private PaymentRepository paymentRepository;

    private GetPaymentUseCase getPayment;

    @BeforeEach
    void setUp() {
        getPayment = new GetPaymentUseCase(paymentRepository);
    }

    @Test
    void shouldReturnExistingPayment() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(approvedPayment()));

        PaymentResponse response = getPayment.execute(new GetPaymentCommand(PAYMENT_ID));

        assertEquals(PAYMENT_ID, response.paymentId());
        assertEquals(ORDER_ID, response.orderId());
        assertEquals(AMOUNT, response.amount());
        assertEquals(PaymentMethod.SIMULATED_CARD, response.paymentMethod());
        assertEquals(PaymentStatus.APPROVED, response.status());
        assertEquals("sim-approved", response.providerReference());
        assertEquals(CREATED_AT, response.createdAt());
        assertEquals(CREATED_AT, response.updatedAt());
        assertNull(response.refundedAt());
        verify(paymentRepository).findById(PAYMENT_ID);
    }

    @Test
    void shouldRejectWhenPaymentDoesNotExist() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> getPayment.execute(new GetPaymentCommand(PAYMENT_ID)));
        verify(paymentRepository).findById(PAYMENT_ID);
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
}
