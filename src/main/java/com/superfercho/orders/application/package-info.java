/**
 * Orders application layer: use cases, consumer-owned ports, and DTOs.
 *
 * {@link com.superfercho.orders.application.usecase.CheckoutUseCase} is the
 * intended local transaction boundary. Infrastructure must wrap checkout
 * execution in a single PostgreSQL transaction so payment, inventory,
 * order persistence, cart clear, and idempotency commit or roll back together.
 */
package com.superfercho.orders.application;
