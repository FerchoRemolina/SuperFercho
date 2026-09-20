package com.superfercho.payments.infrastructure.integration.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.superfercho.orders.application.dto.PaymentRequest;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.payments.application.dto.PaymentResponse;
import com.superfercho.payments.application.dto.ProcessPaymentCommand;
import com.superfercho.payments.domain.model.PaymentMethod;
import com.superfercho.payments.domain.model.PaymentStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentIntegrationMapperTest {

    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");
    private static final UUID PAYMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Money AMOUNT = Money.cop(new BigDecimal("21.00"));

    @Test
    void shouldMapSimulatedCardMethod() {
        assertEquals(
                PaymentMethod.SIMULATED_CARD,
                PaymentIntegrationMapper.toPaymentsPaymentMethod(
                        com.superfercho.orders.application.dto.PaymentMethod.SIMULATED_CARD));
    }

    @Test
    void shouldMapCashOnDeliveryMethod() {
        assertEquals(
                PaymentMethod.CASH_ON_DELIVERY,
                PaymentIntegrationMapper.toPaymentsPaymentMethod(
                        com.superfercho.orders.application.dto.PaymentMethod.CASH_ON_DELIVERY));
    }

    @Test
    void shouldMapPendingStatus() {
        assertEquals(
                com.superfercho.orders.application.dto.PaymentStatus.PENDING,
                PaymentIntegrationMapper.toOrdersPaymentStatus(PaymentStatus.PENDING));
    }

    @Test
    void shouldMapApprovedStatus() {
        assertEquals(
                com.superfercho.orders.application.dto.PaymentStatus.APPROVED,
                PaymentIntegrationMapper.toOrdersPaymentStatus(PaymentStatus.APPROVED));
    }

    @Test
    void shouldMapDeclinedStatus() {
        assertEquals(
                com.superfercho.orders.application.dto.PaymentStatus.DECLINED,
                PaymentIntegrationMapper.toOrdersPaymentStatus(PaymentStatus.DECLINED));
    }

    @Test
    void shouldMapRequestToCommandWithoutStatusOrProviderReference() {
        ProcessPaymentCommand command = PaymentIntegrationMapper.toCommand(
                new PaymentRequest(
                        ORDER_ID, AMOUNT, com.superfercho.orders.application.dto.PaymentMethod.SIMULATED_CARD));

        assertEquals(ORDER_ID, command.orderId());
        assertEquals(AMOUNT, command.amount());
        assertEquals(PaymentMethod.SIMULATED_CARD, command.paymentMethod());
    }

    @Test
    void shouldMapResponseToResult() {
        PaymentResult result = PaymentIntegrationMapper.toResult(new PaymentResponse(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.CASH_ON_DELIVERY,
                PaymentStatus.PENDING,
                "cod-pending",
                NOW,
                NOW,
                null));

        assertEquals(PAYMENT_ID, result.paymentId());
        assertEquals(com.superfercho.orders.application.dto.PaymentStatus.PENDING, result.status());
        assertEquals("cod-pending", result.providerReference());
    }
}
