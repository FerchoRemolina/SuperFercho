package com.superfercho.payments.application.dto;

import java.util.UUID;

public record DeletePaymentCommand(UUID paymentId) {}
