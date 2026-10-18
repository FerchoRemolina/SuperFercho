package com.superfercho.storefront.application.dto;

/**
 * Ajustes del footer destinados al storefront público. {@code qrImageUrl} es
 * null cuando aún no se ha cargado una imagen del QR.
 */
public record StorefrontFooterSettingsResult(
        String supportEmail,
        String linkedinUrl,
        String githubUrl,
        String qrImageUrl) {}
