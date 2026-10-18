package com.superfercho.storefront.infrastructure.rest.dto;

public record UpsertStorefrontFaqRequest(String question, String answer, int displayOrder, boolean active) {}
