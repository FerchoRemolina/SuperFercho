package com.superfercho.catalog.infrastructure.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.superfercho.catalog.application.exception.BarcodeLookupFailedException;
import com.superfercho.catalog.application.exception.BarcodeLookupNotFoundException;
import com.superfercho.catalog.application.exception.CategoryNotFoundException;
import com.superfercho.catalog.application.exception.DuplicateBarcodeException;
import com.superfercho.catalog.application.exception.DuplicateProductTypeException;
import com.superfercho.catalog.application.exception.DuplicateProductVariantException;
import com.superfercho.catalog.application.exception.InvalidBarcodeException;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.exception.ProductTypeNotFoundException;
import com.superfercho.catalog.application.exception.ProductVariantNotFoundException;
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
    void shouldMapDuplicateProductTypeTo409() {
        assertProblem(
                handler.handleDuplicateProductType(new DuplicateProductTypeException()),
                HttpStatus.CONFLICT,
                "DUPLICATE_PRODUCT_TYPE");
    }

    @Test
    void shouldMapDuplicateProductVariantTo409() {
        assertProblem(
                handler.handleDuplicateProductVariant(new DuplicateProductVariantException()),
                HttpStatus.CONFLICT,
                "DUPLICATE_PRODUCT_VARIANT");
    }

    @Test
    void shouldMapCategoryNotFoundTo404() {
        assertProblem(
                handler.handleCategoryNotFound(new CategoryNotFoundException(CATEGORY_ID)),
                HttpStatus.NOT_FOUND,
                "CATEGORY_NOT_FOUND");
    }

    @Test
    void shouldMapProductTypeNotFoundTo404() {
        assertProblem(
                handler.handleProductTypeNotFound(new ProductTypeNotFoundException(CATEGORY_ID)),
                HttpStatus.NOT_FOUND,
                "PRODUCT_TYPE_NOT_FOUND");
    }

    @Test
    void shouldMapProductVariantNotFoundTo404() {
        assertProblem(
                handler.handleProductVariantNotFound(new ProductVariantNotFoundException(PRODUCT_ID)),
                HttpStatus.NOT_FOUND,
                "PRODUCT_VARIANT_NOT_FOUND");
    }

    @Test
    void shouldMapProductNotFoundTo404() {
        assertProblem(
                handler.handleProductNotFound(new ProductNotFoundException(PRODUCT_ID)),
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND");
    }

    @Test
    void shouldMapInvalidBarcodeTo400() {
        assertProblem(
                handler.handleInvalidBarcode(new InvalidBarcodeException("barcode cannot be blank")),
                HttpStatus.BAD_REQUEST,
                "INVALID_BARCODE");
    }

    @Test
    void shouldMapBarcodeLookupNotFoundTo404() {
        assertProblem(
                handler.handleBarcodeLookupNotFound(new BarcodeLookupNotFoundException("000")),
                HttpStatus.NOT_FOUND,
                "BARCODE_LOOKUP_NOT_FOUND");
    }

    @Test
    void shouldMapBarcodeLookupFailedTo502() {
        assertProblem(
                handler.handleBarcodeLookupFailed(new BarcodeLookupFailedException("down")),
                HttpStatus.BAD_GATEWAY,
                "BARCODE_LOOKUP_FAILED");
    }

    private static void assertProblem(ProblemDetail problem, HttpStatus status, String code) {
        assertEquals(status.value(), problem.getStatus());
        assertEquals(status.getReasonPhrase(), problem.getTitle());
        assertEquals(code, problem.getProperties().get("code"));
    }
}
