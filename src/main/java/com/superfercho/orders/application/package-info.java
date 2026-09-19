/**
 * Orders application layer: use cases, consumer-owned ports, and DTOs.
 *
 * External modules consume checkout and cancellation through
 * {@link com.superfercho.orders.application.port.in.CheckoutUseCase} and
 * {@link com.superfercho.orders.application.port.in.CancelOrderUseCase}.
 * {@link com.superfercho.orders.application.usecase.CheckoutUseCase} remains
 * the intended local transaction boundary and must be executed by
 * Infrastructure inside a single PostgreSQL transaction so payment, inventory,
 * order persistence, cart clear, and idempotency commit or roll back together.
 */
package com.superfercho.orders.application;
