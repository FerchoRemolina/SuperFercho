package com.superfercho.payments.application.dto;

import com.superfercho.payments.domain.model.PaymentMethod;
import com.superfercho.payments.domain.model.PaymentStatus;
import com.superfercho.platform.money.Money;
import java.util.UUID;

public record ProcessPaymentCommand(
        UUID orderId,
        Money amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String providerReference) {}
