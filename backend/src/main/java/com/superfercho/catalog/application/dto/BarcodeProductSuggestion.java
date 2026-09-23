package com.superfercho.catalog.application.dto;

public record BarcodeProductSuggestion(
        String barcode, String name, String brand, String description, String imageUrl) {}
