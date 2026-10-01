package com.superfercho.orders.domain.exception;

/**
 * Admin dashboard period is invalid (missing bound, from not before to,
 * unsupported granularity or oversized range).
 */
public class InvalidSalesPeriodException extends RuntimeException {

    public InvalidSalesPeriodException(String message) {
        super(message);
    }
}
