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
    void shouldProcessApprovedSimulatedCardPayment() {
        PaymentResponse response = processPayment.execute(approvedCardCommand("sim-approved"));

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
        assertEquals(response.paymentId(), saved.getValue().id());
        assertEquals(NOW, saved.getValue().createdAt());
        verify(clockPort).currentTime();
    }

    @Test
    void shouldAssignDistinctPaymentIds() {
        UUID firstId = processPayment.execute(approvedCardCommand("sim-1")).paymentId();
        UUID secondId = processPayment.execute(approvedCardCommand("sim-2")).paymentId();

        assertNotNull(firstId);
        assertNotNull(secondId);
        assertNotEquals(firstId, secondId);
    }

    @Test
    void shouldPreservePendingCashOnDeliveryStatus() {
        PaymentResponse response = processPayment.execute(new ProcessPaymentCommand(
                ORDER_ID, AMOUNT, PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING, "cod-pending"));

        assertEquals(PaymentMethod.CASH_ON_DELIVERY, response.paymentMethod());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("cod-pending", response.providerReference());
    }

    @Test
    void shouldAllowNullProviderReference() {
        PaymentResponse response = processPayment.execute(new ProcessPaymentCommand(
                ORDER_ID, AMOUNT, PaymentMethod.SIMULATED_CARD, PaymentStatus.DECLINED, null));

        assertNull(response.providerReference());
        assertEquals(PaymentStatus.DECLINED, response.status());
    }

    private static ProcessPaymentCommand approvedCardCommand(String providerReference) {
        return new ProcessPaymentCommand(
                ORDER_ID, AMOUNT, PaymentMethod.SIMULATED_CARD, PaymentStatus.APPROVED, providerReference);
    }
}
