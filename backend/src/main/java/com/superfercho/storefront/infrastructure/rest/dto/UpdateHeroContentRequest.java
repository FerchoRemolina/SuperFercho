package com.superfercho.storefront.infrastructure.rest.dto;

/** Mirrors UpdateHeroContentRequest. imageUrl nullable = ilustración de branding. */
public record UpdateHeroContentRequest(
        String title,
        String description,
        String ctaLabel,
        String imageUrl,
        String mode) {}
