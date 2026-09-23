package com.superfercho.catalog.infrastructure.integration.openfoodfacts;

import com.superfercho.catalog.application.dto.BarcodeProductSuggestion;
import com.superfercho.catalog.application.exception.BarcodeLookupFailedException;
import com.superfercho.catalog.application.port.ProductBarcodeLookupPort;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

public final class OpenFoodFactsProductLookupAdapter implements ProductBarcodeLookupPort {

    static final String FIELDS = "code,product_name,brands,generic_name,image_front_url,image_url";

    private final RestClient restClient;
    private final OpenFoodFactsProperties properties;

    public OpenFoodFactsProductLookupAdapter(RestClient restClient, OpenFoodFactsProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public Optional<BarcodeProductSuggestion> findByBarcode(String barcode) {
        URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/api/v3/product/{barcode}")
                .queryParam("fields", FIELDS)
                .buildAndExpand(barcode)
                .toUri();
        OpenFoodFactsProductResponse response;
        try {
            response = restClient
                    .get()
                    .uri(uri)
                    .header(HttpHeaders.USER_AGENT, properties.userAgent())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(OpenFoodFactsProductResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new BarcodeLookupFailedException("barcode lookup provider request failed");
        } catch (RestClientException exception) {
            if (isTimeout(exception)) {
                throw new BarcodeLookupFailedException("barcode lookup provider request timed out", exception);
            }
            throw new BarcodeLookupFailedException("barcode lookup provider request failed", exception);
        }
        return toSuggestion(barcode, response);
    }

    private static Optional<BarcodeProductSuggestion> toSuggestion(
            String requestedBarcode, OpenFoodFactsProductResponse response) {
        if (response == null) {
            return Optional.empty();
        }
        if (isNotFound(response)) {
            return Optional.empty();
        }
        OpenFoodFactsProduct product = response.product();
        if (product == null) {
            return Optional.empty();
        }
        String code = blankToNull(product.code());
        String barcode = code == null ? requestedBarcode : code;
        return Optional.of(new BarcodeProductSuggestion(
                barcode,
                blankToNull(product.productName()),
                firstBrand(product.brands()),
                blankToNull(product.genericName()),
                firstNonBlank(product.imageFrontUrl(), product.imageUrl())));
    }

    private static boolean isNotFound(OpenFoodFactsProductResponse response) {
        if (response.result() != null && response.result().id() != null) {
            String id = response.result().id().toLowerCase(Locale.ROOT);
            if (id.contains("not_found") || id.contains("product_not_found")) {
                return true;
            }
        }
        if (response.status() != null) {
            String status = response.status().trim().toLowerCase(Locale.ROOT);
            if ("failure".equals(status) || "0".equals(status)) {
                return true;
            }
        }
        return false;
    }

    private static String firstBrand(String brands) {
        String normalized = blankToNull(brands);
        if (normalized == null) {
            return null;
        }
        return Arrays.stream(normalized.split("[,;]"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .findFirst()
                .orElse(null);
    }

    private static String firstNonBlank(String primary, String fallback) {
        String first = blankToNull(primary);
        return first != null ? first : blankToNull(fallback);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isTimeout(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
