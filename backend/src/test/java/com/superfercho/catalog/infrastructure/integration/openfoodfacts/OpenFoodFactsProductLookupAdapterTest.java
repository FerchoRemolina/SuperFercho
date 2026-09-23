package com.superfercho.catalog.infrastructure.integration.openfoodfacts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.superfercho.catalog.application.dto.BarcodeProductSuggestion;
import com.superfercho.catalog.application.exception.BarcodeLookupFailedException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenFoodFactsProductLookupAdapterTest {

    private static final OpenFoodFactsProperties PROPERTIES = new OpenFoodFactsProperties(
            "https://world.openfoodfacts.org", "SuperFercho/0.1 (https://example.test)");

    private MockRestServiceServer server;
    private OpenFoodFactsProductLookupAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new OpenFoodFactsProductLookupAdapter(builder.build(), PROPERTIES);
    }

    @Test
    void mapsV3FixtureAndSendsUserAgentAndFields() {
        server.expect(requestTo(
                        "https://world.openfoodfacts.org/api/v3/product/3017620422003?fields="
                                + OpenFoodFactsProductLookupAdapter.FIELDS))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.USER_AGENT, "SuperFercho/0.1 (https://example.test)"))
                .andRespond(withSuccess(
                        """
                        {
                          "status":"success",
                          "product":{
                            "code":"3017620422003",
                            "product_name":"Nutella",
                            "brands":"Ferrero, Other",
                            "generic_name":"Hazelnut cocoa spread",
                            "image_front_url":"https://img.test/front.png",
                            "image_url":"https://img.test/fallback.png"
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        Optional<BarcodeProductSuggestion> suggestion = adapter.findByBarcode("3017620422003");

        server.verify();
        assertTrue(suggestion.isPresent());
        assertEquals("3017620422003", suggestion.get().barcode());
        assertEquals("Nutella", suggestion.get().name());
        assertEquals("Ferrero", suggestion.get().brand());
        assertEquals("Hazelnut cocoa spread", suggestion.get().description());
        assertEquals("https://img.test/front.png", suggestion.get().imageUrl());
    }

    @Test
    void allowsMissingOptionalFieldsAndFallsBackImageUrl() {
        server.expect(requestTo(
                        "https://world.openfoodfacts.org/api/v3/product/12345678?fields="
                                + OpenFoodFactsProductLookupAdapter.FIELDS))
                .andRespond(withSuccess(
                        """
                        {
                          "status":"success",
                          "product":{
                            "code":"12345678",
                            "product_name":"Solo nombre",
                            "image_url":"https://img.test/only.png"
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        BarcodeProductSuggestion suggestion = adapter.findByBarcode("12345678").orElseThrow();

        assertEquals("Solo nombre", suggestion.name());
        assertEquals(null, suggestion.brand());
        assertEquals(null, suggestion.description());
        assertEquals("https://img.test/only.png", suggestion.imageUrl());
    }

    @Test
    void returnsEmptyWhenProductNotFoundEnvelope() {
        server.expect(requestTo(
                        "https://world.openfoodfacts.org/api/v3/product/00000000?fields="
                                + OpenFoodFactsProductLookupAdapter.FIELDS))
                .andRespond(withSuccess(
                        """
                        {
                          "status":"failure",
                          "result":{"id":"product_not_found","name":"Product not found"}
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        assertTrue(adapter.findByBarcode("00000000").isEmpty());
    }

    @Test
    void returnsEmptyOnHttp404() {
        server.expect(requestTo(
                        "https://world.openfoodfacts.org/api/v3/product/00000000?fields="
                                + OpenFoodFactsProductLookupAdapter.FIELDS))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertTrue(adapter.findByBarcode("00000000").isEmpty());
    }

    @Test
    void mapsProvider5xxToLookupFailed() {
        server.expect(requestTo(
                        "https://world.openfoodfacts.org/api/v3/product/3017620422003?fields="
                                + OpenFoodFactsProductLookupAdapter.FIELDS))
                .andRespond(withServerError());

        assertThrows(BarcodeLookupFailedException.class, () -> adapter.findByBarcode("3017620422003"));
    }
}
