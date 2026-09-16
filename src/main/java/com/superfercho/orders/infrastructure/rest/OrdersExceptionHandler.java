package com.superfercho.orders.infrastructure.rest;

import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import com.superfercho.orders.application.exception.AddressNotAvailableException;
import com.superfercho.orders.application.exception.CartEmptyException;
import com.superfercho.orders.application.exception.IdempotencyConflictException;
import com.superfercho.orders.application.exception.InvalidCheckoutException;
import com.superfercho.orders.application.exception.InvalidOrderStatusUpdateException;
import com.superfercho.orders.application.exception.OrderNotFoundException;
import com.superfercho.orders.application.exception.OrderOwnershipException;
import com.superfercho.orders.application.exception.PaymentDeclinedException;
import com.superfercho.orders.application.exception.ProductNotAvailableException;
import com.superfercho.orders.application.exception.ProductPriceChangedException;
import com.superfercho.orders.application.exception.StockUnavailableException;
import com.superfercho.orders.domain.exception.InvalidOrderException;
import com.superfercho.orders.domain.exception.InvalidOrderItemException;
import com.superfercho.orders.domain.exception.InvalidOrderStateTransitionException;
import com.superfercho.orders.domain.exception.OrderCancellationNotAllowedException;
import com.superfercho.payments.application.exception.PaymentNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrdersExceptionHandler {

    @ExceptionHandler(UnauthenticatedUserException.class)
    ProblemDetail handleUnauthenticated(UnauthenticatedUserException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", exception.getMessage());
    }

    @ExceptionHandler({OrderNotFoundException.class, OrderOwnershipException.class})
    ProblemDetail handleOrderNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidCheckoutException.class)
    ProblemDetail handleInvalidCheckout(InvalidCheckoutException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_CHECKOUT", exception.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail handleIdempotencyConflict(IdempotencyConflictException exception) {
        return problem(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(ProductPriceChangedException.class)
    ProblemDetail handleProductPriceChanged(ProductPriceChangedException exception) {
        return problem(HttpStatus.CONFLICT, "PRODUCT_PRICE_CHANGED", exception.getMessage());
    }

    @ExceptionHandler(StockUnavailableException.class)
    ProblemDetail handleStockUnavailable(StockUnavailableException exception) {
        return problem(HttpStatus.CONFLICT, "STOCK_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(PaymentDeclinedException.class)
    ProblemDetail handlePaymentDeclined(PaymentDeclinedException exception) {
        return problem(HttpStatus.CONFLICT, "PAYMENT_DECLINED", exception.getMessage());
    }

    @ExceptionHandler(CartEmptyException.class)
    ProblemDetail handleCartEmpty(CartEmptyException exception) {
        return problem(HttpStatus.CONFLICT, "CART_EMPTY", exception.getMessage());
    }

    @ExceptionHandler(AddressNotAvailableException.class)
    ProblemDetail handleAddressNotAvailable(AddressNotAvailableException exception) {
        return problem(HttpStatus.CONFLICT, "ADDRESS_NOT_AVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(ProductNotAvailableException.class)
    ProblemDetail handleProductNotAvailable(ProductNotAvailableException exception) {
        return problem(HttpStatus.CONFLICT, "PRODUCT_NOT_AVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(OrderCancellationNotAllowedException.class)
    ProblemDetail handleCancellationNotAllowed(OrderCancellationNotAllowedException exception) {
        return problem(HttpStatus.CONFLICT, "CANCELLATION_NOT_ALLOWED", exception.getMessage());
    }

    @ExceptionHandler(InvalidOrderStateTransitionException.class)
    ProblemDetail handleInvalidOrderTransition(InvalidOrderStateTransitionException exception) {
        return problem(HttpStatus.CONFLICT, "INVALID_ORDER_TRANSITION", exception.getMessage());
    }

    @ExceptionHandler(InvalidOrderStatusUpdateException.class)
    ProblemDetail handleInvalidOrderStatusUpdate(InvalidOrderStatusUpdateException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_ORDER_STATUS_UPDATE", exception.getMessage());
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    ProblemDetail handlePaymentNotFound(PaymentNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({InvalidOrderException.class, InvalidOrderItemException.class})
    ProblemDetail handleInvalidOrder(RuntimeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_ORDER", exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(status.getReasonPhrase());
        problem.setDetail(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
