package com.superfercho.storefront.infrastructure.rest.dto;

import com.superfercho.storefront.domain.model.StorefrontFaq;
import java.util.UUID;

/** Respuesta administrativa: incluye el estado activo/inactivo. */
public record FaqAdminRestResponse(
        UUID id,
        String question,
        String answer,
        int displayOrder,
        boolean active) {

    public static FaqAdminRestResponse from(StorefrontFaq faq) {
        return new FaqAdminRestResponse(
                faq.id(), faq.question(), faq.answer(), faq.displayOrder(), faq.active());
    }
}
