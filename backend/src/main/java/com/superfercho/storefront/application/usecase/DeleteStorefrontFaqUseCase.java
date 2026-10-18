package com.superfercho.storefront.application.usecase;

import com.superfercho.storefront.application.exception.StorefrontContentNotFoundException;
import com.superfercho.storefront.application.port.StorefrontFaqRepository;
import java.util.Objects;
import java.util.UUID;

/** Eliminación de una pregunta frecuente (ADMIN). */
public final class DeleteStorefrontFaqUseCase {

    private final StorefrontFaqRepository faqRepository;

    public DeleteStorefrontFaqUseCase(StorefrontFaqRepository faqRepository) {
        this.faqRepository = Objects.requireNonNull(faqRepository, "faqRepository");
    }

    public void execute(UUID faqId) {
        Objects.requireNonNull(faqId, "faqId");
        if (faqRepository.findById(faqId).isEmpty()) {
            throw new StorefrontContentNotFoundException(
                    "la pregunta frecuente no existe: " + faqId);
        }
        faqRepository.deleteById(faqId);
    }
}
