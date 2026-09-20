package com.superfercho.payments.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.payments.application.dto.PaymentResponse;
import com.superfercho.payments.application.dto.ProcessPaymentCommand;
import com.superfercho.payments.application.port.ClockPort;
import com.superfercho.payments.application.port.PaymentRepository;
import com.superfercho.payments.domain.model.Payment;
import com.superfercho.payments.domain.model.PaymentMethod;
import com.superfercho.payments.domain.model.PaymentStatus;
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
class ProcessPaymentUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Money AMOUNT = Money.cop(new BigDecimal("21.00"));

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ClockPort clockPort;

    private ProcessPaymentUseCase processPayment;

    @BeforeEach
    void setUp() {
        processPayment = new ProcessPaymentUseCase(paymentRepository, clockPort);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldApproveSimulatedCardPayment() {
        PaymentResponse response = processPayment.execute(cardCommand());

        assertNotNull(response.paymentId());
        assertEquals(ORDER_ID, response.orderId());
        assertEquals(AMOUNT, response.amount());
        assertEquals(PaymentMethod.SIMULATED_CARD, response.paymentMethod());
        assertEquals(PaymentStatus.APPROVED, response.status());
        assertEquals("sim-approved", response.providerReference());
        assertEquals(NOW, response.createdAt());
        assertEquals(NOW, response.updatedAt());
        assertNull(response.refundedAt());

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        Payment payment = saved.getValue();
        assertEquals(response.paymentId(), payment.id());
        assertEquals(ORDER_ID, payment.orderId());
        assertEquals(AMOUNT, payment.amount());
        assertEquals(PaymentMethod.SIMULATED_CARD, payment.paymentMethod());
        assertEquals(PaymentStatus.APPROVED, payment.status());
        assertEquals("sim-approved", payment.providerReference());
        assertEquals(NOW, payment.createdAt());
        verify(clockPort).currentTime();
    }

    @Test
    void shouldKeepCashOnDeliveryPending() {
        PaymentResponse response = processPayment.execute(codCommand());

        assertEquals(ORDER_ID, response.orderId());
        assertEquals(AMOUNT, response.amount());
        assertEquals(PaymentMethod.CASH_ON_DELIVERY, response.paymentMethod());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("cod-pending", response.providerReference());

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        Payment payment = saved.getValue();
        assertEquals(ORDER_ID, payment.orderId());
        assertEquals(AMOUNT, payment.amount());
        assertEquals(PaymentMethod.CASH_ON_DELIVERY, payment.paymentMethod());
        assertEquals(PaymentStatus.PENDING, payment.status());
        assertEquals("cod-pending", payment.providerReference());
        verify(clockPort).currentTime();
    }

    @Test
    void shouldAssignDistinctPaymentIds() {
        UUID firstId = processPayment.execute(cardCommand()).paymentId();
        UUID secondId = processPayment.execute(cardCommand()).paymentId();

        assertNotNull(firstId);
        assertNotNull(secondId);
        assertNotEquals(firstId, secondId);
    }

    private static ProcessPaymentCommand cardCommand() {
        return new ProcessPaymentCommand(ORDER_ID, AMOUNT, PaymentMethod.SIMULATED_CARD);
    }

    private static ProcessPaymentCommand codCommand() {
        return new ProcessPaymentCommand(ORDER_ID, AMOUNT, PaymentMethod.CASH_ON_DELIVERY);
    }
}
