package com.superfercho.catalog.infrastructure.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.superfercho.catalog.application.exception.CategoryNotFoundException;
import com.superfercho.catalog.application.exception.DuplicateBarcodeException;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.domain.exception.InvalidCategoryException;
import com.superfercho.catalog.domain.exception.InvalidProductException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class CatalogExceptionHandlerTest {

    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final CatalogExceptionHandler handler = new CatalogExceptionHandler();

    @Test
    void shouldMapInvalidCategoryTo400() {
        assertProblem(
                handler.handleInvalidCategory(new InvalidCategoryException("name cannot be null or blank")),
                HttpStatus.BAD_REQUEST,
                "INVALID_CATEGORY");
    }

    @Test
    void shouldMapInvalidProductTo400() {
        assertProblem(
                handler.handleInvalidProduct(new InvalidProductException("name cannot be null or blank")),
                HttpStatus.BAD_REQUEST,
                "INVALID_PRODUCT");
    }

    @Test
    void shouldMapInvalidCategoryReferenceTo400() {
        assertProblem(
                handler.handleInvalidCategoryReference(new InvalidCategoryReferenceException(CATEGORY_ID)),
                HttpStatus.BAD_REQUEST,
                "INVALID_CATEGORY_REFERENCE");
    }

    @Test
    void shouldMapDuplicateBarcodeTo409() {
        assertProblem(
                handler.handleDuplicateBarcode(new DuplicateBarcodeException()),
                HttpStatus.CONFLICT,
                "DUPLICATE_BARCODE");
    }

    @Test
    void shouldMapCategoryNotFoundTo404() {
        assertProblem(
                handler.handleCategoryNotFound(new CategoryNotFoundException(CATEGORY_ID)),
                HttpStatus.NOT_FOUND,
                "CATEGORY_NOT_FOUND");
    }

    @Test
    void shouldMapProductNotFoundTo404() {
        assertProblem(
                handler.handleProductNotFound(new ProductNotFoundException(PRODUCT_ID)),
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND");
    }

    private static void assertProblem(ProblemDetail problem, HttpStatus status, String code) {
        assertEquals(status.value(), problem.getStatus());
        assertEquals(status.getReasonPhrase(), problem.getTitle());
        assertEquals(code, problem.getProperties().get("code"));
    }
}
