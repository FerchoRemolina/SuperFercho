package com.superfercho.catalog.infrastructure.rest.dto;

import com.superfercho.catalog.application.dto.BarcodeProductSuggestion;

public record BarcodeProductSuggestionRestResponse(
        String barcode, String name, String brand, String description, String imageUrl) {

    public static BarcodeProductSuggestionRestResponse from(BarcodeProductSuggestion suggestion) {
        return new BarcodeProductSuggestionRestResponse(
                suggestion.barcode(),
                suggestion.name(),
                suggestion.brand(),
                suggestion.description(),
                suggestion.imageUrl());
    }
}
