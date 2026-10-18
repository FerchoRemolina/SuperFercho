package com.superfercho.storefront.infrastructure.rest.dto;

import com.superfercho.storefront.application.dto.StorefrontFooterSettingsResult;

/** Ajustes del footer para el storefront público. */
public record FooterSettingsRestResponse(
        String supportEmail,
        String linkedinUrl,
        String githubUrl,
        String qrImageUrl) {

    public static FooterSettingsRestResponse from(StorefrontFooterSettingsResult settings) {
        return new FooterSettingsRestResponse(
                settings.supportEmail(),
                settings.linkedinUrl(),
                settings.githubUrl(),
                settings.qrImageUrl());
    }
}
