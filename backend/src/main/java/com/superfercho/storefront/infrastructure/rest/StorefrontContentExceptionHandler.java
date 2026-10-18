package com.superfercho.storefront.infrastructure.rest;

import com.superfercho.storefront.application.exception.StorefrontContentNotFoundException;
import com.superfercho.storefront.domain.exception.InvalidStorefrontContentException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StorefrontContentExceptionHandler {

    @ExceptionHandler(InvalidStorefrontContentException.class)
    ProblemDetail handleInvalidStorefrontContent(InvalidStorefrontContentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_STOREFRONT_CONTENT", exception.getMessage());
    }

    @ExceptionHandler(StorefrontContentNotFoundException.class)
    ProblemDetail handleStorefrontContentNotFound(StorefrontContentNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "STOREFRONT_CONTENT_NOT_FOUND", exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        if (detail != null) {
            problem.setDetail(detail);
        }
        return problem;
    }
}
