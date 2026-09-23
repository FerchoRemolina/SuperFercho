package com.superfercho.catalog.infrastructure.integration.openfoodfacts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenFoodFactsProductResponse(
        String status, @JsonProperty("result") OpenFoodFactsResult result, OpenFoodFactsProduct product) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenFoodFactsResult(String id, String name) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenFoodFactsProduct(
        String code,
        @JsonProperty("product_name") String productName,
        String brands,
        @JsonProperty("generic_name") String genericName,
        @JsonProperty("image_front_url") String imageFrontUrl,
        @JsonProperty("image_url") String imageUrl) {}
