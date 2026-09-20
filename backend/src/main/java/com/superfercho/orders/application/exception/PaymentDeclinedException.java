package com.superfercho.orders.application.exception;

public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException() {
        super("Payment was declined");
    }
}
