package com.superfercho.catalog.infrastructure.rest;

import com.superfercho.catalog.application.exception.CategoryNotFoundException;
import com.superfercho.catalog.application.exception.DuplicateBarcodeException;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.domain.exception.InvalidCategoryException;
import com.superfercho.catalog.domain.exception.InvalidProductException;
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
public class CatalogExceptionHandler {

    @ExceptionHandler(InvalidCategoryException.class)
    ProblemDetail handleInvalidCategory(InvalidCategoryException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY", exception.getMessage());
    }

    @ExceptionHandler(InvalidProductException.class)
    ProblemDetail handleInvalidProduct(InvalidProductException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT", exception.getMessage());
    }

    @ExceptionHandler(InvalidCategoryReferenceException.class)
    ProblemDetail handleInvalidCategoryReference(InvalidCategoryReferenceException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY_REFERENCE", exception.getMessage());
    }

    @ExceptionHandler(DuplicateBarcodeException.class)
    ProblemDetail handleDuplicateBarcode(DuplicateBarcodeException exception) {
        return problem(HttpStatus.CONFLICT, "DUPLICATE_BARCODE", exception.getMessage());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    ProblemDetail handleCategoryNotFound(CategoryNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(status.getReasonPhrase());
        problem.setDetail(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
