package com.superfercho.catalog.application.dto;

public record SearchProductsCommand(String text, CatalogView view) {
}
