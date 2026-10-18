package com.superfercho.storefront.infrastructure.rest.dto;

import com.superfercho.storefront.domain.model.StorefrontFaq;
import java.util.UUID;

/** Respuesta pública de una pregunta frecuente activa. Sin campos de admin. */
public record FaqPublicRestResponse(
        UUID id,
        String question,
        String answer,
        int displayOrder) {

    public static FaqPublicRestResponse from(StorefrontFaq faq) {
        return new FaqPublicRestResponse(faq.id(), faq.question(), faq.answer(), faq.displayOrder());
    }
}
