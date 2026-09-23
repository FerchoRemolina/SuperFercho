package com.superfercho.catalog.infrastructure.integration.openfoodfacts;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "superfercho.catalog.openfoodfacts")
public record OpenFoodFactsProperties(String baseUrl, String userAgent) {

    public static final String DEFAULT_BASE_URL = "https://world.openfoodfacts.org";
    public static final String DEFAULT_USER_AGENT = "SuperFercho/0.1 (https://github.com/FerchoRemolina/SuperFercho)";

    public OpenFoodFactsProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : trimTrailingSlash(baseUrl.trim());
        userAgent = userAgent == null || userAgent.isBlank() ? DEFAULT_USER_AGENT : userAgent.trim();
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
