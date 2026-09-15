package com.superfercho.shopping.infrastructure.rest;

import com.superfercho.shopping.application.exception.CartNotFoundException;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.exception.ShoppingListNotFoundException;
import com.superfercho.shopping.domain.exception.InvalidCartException;
import com.superfercho.shopping.domain.exception.InvalidCartItemException;
import com.superfercho.shopping.domain.exception.InvalidShoppingListException;
import com.superfercho.shopping.domain.exception.InvalidShoppingListItemException;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ShoppingExceptionHandler {

    @ExceptionHandler(CartNotFoundException.class)
    ProblemDetail handleCartNotFound(CartNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "CART_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ShoppingListNotFoundException.class)
    ProblemDetail handleShoppingListNotFound(ShoppingListNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "SHOPPING_LIST_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidCartException.class)
    ProblemDetail handleInvalidCart(InvalidCartException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_CART", exception.getMessage());
    }

    @ExceptionHandler(InvalidCartItemException.class)
    ProblemDetail handleInvalidCartItem(InvalidCartItemException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_CART_ITEM", exception.getMessage());
    }

    @ExceptionHandler(InvalidShoppingListException.class)
    ProblemDetail handleInvalidShoppingList(InvalidShoppingListException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_SHOPPING_LIST", exception.getMessage());
    }

    @ExceptionHandler(InvalidShoppingListItemException.class)
    ProblemDetail handleInvalidShoppingListItem(InvalidShoppingListItemException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_SHOPPING_LIST_ITEM", exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(status.getReasonPhrase());
        problem.setDetail(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
