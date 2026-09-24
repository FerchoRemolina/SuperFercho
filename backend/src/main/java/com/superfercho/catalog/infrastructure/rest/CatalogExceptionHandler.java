package com.superfercho.catalog.infrastructure.rest;

import com.superfercho.catalog.application.exception.BarcodeLookupFailedException;
import com.superfercho.catalog.application.exception.BarcodeLookupNotFoundException;
import com.superfercho.catalog.application.exception.CategoryNotFoundException;
import com.superfercho.catalog.application.exception.DuplicateBarcodeException;
import com.superfercho.catalog.application.exception.DuplicateProductTypeException;
import com.superfercho.catalog.application.exception.DuplicateProductVariantException;
import com.superfercho.catalog.application.exception.InvalidBarcodeException;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.exception.InvalidProductTypeReferenceException;
import com.superfercho.catalog.application.exception.InvalidProductVariantReferenceException;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.exception.ProductStockConflictException;
import com.superfercho.catalog.application.exception.ProductTypeNotFoundException;
import com.superfercho.catalog.application.exception.ProductVariantNotFoundException;
import com.superfercho.catalog.domain.exception.InvalidCategoryException;
import com.superfercho.catalog.domain.exception.InvalidPresentationException;
import com.superfercho.catalog.domain.exception.InvalidProductException;
import com.superfercho.catalog.domain.exception.InvalidProductTypeException;
import com.superfercho.catalog.domain.exception.InvalidProductVariantException;
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

    @ExceptionHandler(InvalidPresentationException.class)
    ProblemDetail handleInvalidPresentation(InvalidPresentationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PRESENTATION", exception.getMessage());
    }

    @ExceptionHandler(InvalidProductTypeException.class)
    ProblemDetail handleInvalidProductType(InvalidProductTypeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_TYPE", exception.getMessage());
    }

    @ExceptionHandler(InvalidProductVariantException.class)
    ProblemDetail handleInvalidProductVariant(InvalidProductVariantException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_VARIANT", exception.getMessage());
    }

    @ExceptionHandler(InvalidCategoryReferenceException.class)
    ProblemDetail handleInvalidCategoryReference(InvalidCategoryReferenceException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY_REFERENCE", exception.getMessage());
    }

    @ExceptionHandler(InvalidProductTypeReferenceException.class)
    ProblemDetail handleInvalidProductTypeReference(InvalidProductTypeReferenceException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_TYPE_REFERENCE", exception.getMessage());
    }

    @ExceptionHandler(InvalidProductVariantReferenceException.class)
    ProblemDetail handleInvalidProductVariantReference(InvalidProductVariantReferenceException exception) {
        return problem(
                HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_VARIANT_REFERENCE", exception.getMessage());
    }

    @ExceptionHandler(DuplicateBarcodeException.class)
    ProblemDetail handleDuplicateBarcode(DuplicateBarcodeException exception) {
        return problem(HttpStatus.CONFLICT, "DUPLICATE_BARCODE", exception.getMessage());
    }

    @ExceptionHandler(DuplicateProductTypeException.class)
    ProblemDetail handleDuplicateProductType(DuplicateProductTypeException exception) {
        return problem(HttpStatus.CONFLICT, "DUPLICATE_PRODUCT_TYPE", exception.getMessage());
    }

    @ExceptionHandler(DuplicateProductVariantException.class)
    ProblemDetail handleDuplicateProductVariant(DuplicateProductVariantException exception) {
        return problem(HttpStatus.CONFLICT, "DUPLICATE_PRODUCT_VARIANT", exception.getMessage());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    ProblemDetail handleCategoryNotFound(CategoryNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ProductTypeNotFoundException.class)
    ProblemDetail handleProductTypeNotFound(ProductTypeNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "PRODUCT_TYPE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ProductVariantNotFoundException.class)
    ProblemDetail handleProductVariantNotFound(ProductVariantNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "PRODUCT_VARIANT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ProductStockConflictException.class)
    ProblemDetail handleProductStockConflict(ProductStockConflictException exception) {
        return problem(HttpStatus.CONFLICT, "PRODUCT_STOCK_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(InvalidBarcodeException.class)
    ProblemDetail handleInvalidBarcode(InvalidBarcodeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_BARCODE", exception.getMessage());
    }

    @ExceptionHandler(BarcodeLookupNotFoundException.class)
    ProblemDetail handleBarcodeLookupNotFound(BarcodeLookupNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "BARCODE_LOOKUP_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(BarcodeLookupFailedException.class)
    ProblemDetail handleBarcodeLookupFailed(BarcodeLookupFailedException ignored) {
        return problem(HttpStatus.BAD_GATEWAY, "BARCODE_LOOKUP_FAILED", "Barcode lookup provider request failed");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(status.getReasonPhrase());
        problem.setDetail(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
