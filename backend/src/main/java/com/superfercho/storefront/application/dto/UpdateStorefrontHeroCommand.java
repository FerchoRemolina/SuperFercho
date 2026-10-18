package com.superfercho.storefront.application.dto;

import com.superfercho.storefront.domain.model.HeroPresentationMode;

public record UpdateStorefrontHeroCommand(
        String title,
        String description,
        String ctaLabel,
        String imageUrl,
        HeroPresentationMode mode) {

    public static UpdateStorefrontHeroCommand of(
            String title,
            String description,
            String ctaLabel,
            String imageUrl,
            String rawMode) {
        return new UpdateStorefrontHeroCommand(
                title, description, ctaLabel, imageUrl, HeroPresentationMode.parse(rawMode));
    }
}
