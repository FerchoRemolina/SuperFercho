package com.superfercho.storefront.infrastructure.rest.dto;

import com.superfercho.storefront.application.dto.StorefrontHeroResult;

/** Mirrors el contenido público del Hero. imageUrl es null cuando aplica la
 *  ilustración de branding por defecto. */
public record HeroContentRestResponse(
        String title,
        String description,
        String ctaLabel,
        String imageUrl,
        String mode,
        java.time.Instant updatedAt) {

    public static HeroContentRestResponse from(StorefrontHeroResult result) {
        return new HeroContentRestResponse(
                result.title(),
                result.description(),
                result.ctaLabel(),
                result.imageUrl(),
                result.mode().name(),
                result.updatedAt());
    }
}
