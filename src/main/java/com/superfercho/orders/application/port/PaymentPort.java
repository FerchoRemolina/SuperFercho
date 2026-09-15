package com.superfercho.orders.application.port;

import com.superfercho.orders.application.dto.PaymentRequest;
import com.superfercho.orders.application.dto.PaymentResult;
import java.util.UUID;

public interface PaymentPort {

    PaymentResult processPayment(PaymentRequest request);

    PaymentResult getPayment(UUID paymentId);

    void refundPayment(UUID paymentId);
}
