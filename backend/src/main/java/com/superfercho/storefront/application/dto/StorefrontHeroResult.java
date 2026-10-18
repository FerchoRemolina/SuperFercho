package com.superfercho.storefront.application.dto;

import com.superfercho.storefront.domain.model.HeroPresentationMode;
import java.time.Instant;

public record StorefrontHeroResult(
        String title,
        String description,
        String ctaLabel,
        String imageUrl,
        HeroPresentationMode mode,
        Instant updatedAt) {

    public static StorefrontHeroResult from(com.superfercho.storefront.domain.model.StorefrontContent content) {
        return new StorefrontHeroResult(
                content.heroTitle(),
                content.heroDescription(),
                content.heroCtaLabel(),
                content.heroImageUrl(),
                content.heroMode(),
                content.updatedAt());
    }
}
