package com.superfercho.assistant.infrastructure.configuration;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tarifas para el costo ESTIMADO de IA. Si alguna tarifa no está configurada,
 * el costo se reporta como no calculable (null); nunca se inventa.
 */
@ConfigurationProperties(prefix = "assistant.fercho.cost")
public record FerchoCostProperties(
        String model,
        BigDecimal promptPer1k,
        BigDecimal completionPer1k,
        String currency
) {

    public FerchoCostProperties {
        model = model == null || model.isBlank() ? null : model;
        currency = currency == null || currency.isBlank() ? "USD" : currency;
    }

    public boolean calculable() {
        return promptPer1k != null && completionPer1k != null;
    }
}
