package com.superfercho.orders.infrastructure.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.superfercho.catalog.application.dto.UnavailableProduct;
import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import com.superfercho.orders.application.exception.IdempotencyConflictException;
import com.superfercho.orders.application.exception.InvalidCheckoutException;
import com.superfercho.orders.application.exception.InvalidOrderStatusUpdateException;
import com.superfercho.orders.application.exception.OrderNotFoundException;
import com.superfercho.orders.application.exception.OrderOwnershipException;
import com.superfercho.orders.application.exception.PaymentDeclinedException;
import com.superfercho.orders.application.exception.ProductPriceChangedException;
import com.superfercho.orders.application.exception.StockUnavailableException;
import com.superfercho.orders.domain.exception.InvalidOrderStateTransitionException;
import com.superfercho.orders.domain.exception.OrderCancellationNotAllowedException;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.payments.application.exception.PaymentNotFoundException;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class OrdersExceptionHandlerTest {

    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PAYMENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));

    private final OrdersExceptionHandler handler = new OrdersExceptionHandler();

    @Test
    void shouldMapUnauthenticatedTo401() {
        assertProblem(handler.handleUnauthenticated(new UnauthenticatedUserException()), HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
    }

    @Test
    void shouldMapOrderNotFoundTo404() {
        assertProblem(handler.handleOrderNotFound(new OrderNotFoundException(ORDER_ID)), HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND");
    }

    @Test
    void shouldMapOrderOwnershipTo404WithoutDedicatedCode() {
        assertProblem(
                handler.handleOrderNotFound(new OrderOwnershipException(CUSTOMER_ID, ORDER_ID)),
                HttpStatus.NOT_FOUND,
                "ORDER_NOT_FOUND");
    }

    @Test
    void shouldMapInvalidCheckoutTo400() {
        assertProblem(
                handler.handleInvalidCheckout(new InvalidCheckoutException("idempotencyKey cannot be null or blank")),
                HttpStatus.BAD_REQUEST,
                "INVALID_CHECKOUT");
    }

    @Test
    void shouldMapIdempotencyConflictTo409() {
        assertProblem(
                handler.handleIdempotencyConflict(new IdempotencyConflictException("checkout-key-1")),
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_CONFLICT");
    }

    @Test
    void shouldMapProductPriceChangedTo409() {
        assertProblem(
                handler.handleProductPriceChanged(new ProductPriceChangedException(PRODUCT_ID, PRICE, PRICE)),
                HttpStatus.CONFLICT,
                "PRODUCT_PRICE_CHANGED");
    }

    @Test
    void shouldMapStockUnavailableTo409() {
        assertProblem(
                handler.handleStockUnavailable(
                        new StockUnavailableException(List.of(new UnavailableProduct(PRODUCT_ID, 2)))),
                HttpStatus.CONFLICT,
                "STOCK_UNAVAILABLE");
    }

    @Test
    void shouldMapPaymentDeclinedTo409() {
        assertProblem(handler.handlePaymentDeclined(new PaymentDeclinedException()), HttpStatus.CONFLICT, "PAYMENT_DECLINED");
    }

    @Test
    void shouldMapCancellationNotAllowedTo409() {
        assertProblem(
                handler.handleCancellationNotAllowed(
                        new OrderCancellationNotAllowedException("customer cancellation window has expired")),
                HttpStatus.CONFLICT,
                "CANCELLATION_NOT_ALLOWED");
    }

    @Test
    void shouldMapInvalidOrderStateTransitionTo409() {
        assertProblem(
                handler.handleInvalidOrderTransition(
                        new InvalidOrderStateTransitionException(OrderStatus.CANCELLED, OrderStatus.PENDING)),
                HttpStatus.CONFLICT,
                "INVALID_ORDER_TRANSITION");
    }

    @Test
    void shouldMapInvalidOrderStatusUpdateTo400() {
        assertProblem(
                handler.handleInvalidOrderStatusUpdate(new InvalidOrderStatusUpdateException(OrderStatus.CANCELLED)),
                HttpStatus.BAD_REQUEST,
                "INVALID_ORDER_STATUS_UPDATE");
    }

    @Test
    void shouldMapPaymentNotFoundTo404() {
        assertProblem(
                handler.handlePaymentNotFound(new PaymentNotFoundException(PAYMENT_ID)),
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND");
    }

    private static void assertProblem(ProblemDetail problem, HttpStatus status, String code) {
        assertEquals(status.value(), problem.getStatus());
        assertEquals(status.getReasonPhrase(), problem.getTitle());
        assertEquals(code, problem.getProperties().get("code"));
    }
}
