package com.superfercho.storefront.application.dto;

/** Ajustes del footer editables por el Admin. */
public record UpdateStorefrontFooterSettingsCommand(
        String supportEmail,
        String linkedinUrl,
        String githubUrl) {}
