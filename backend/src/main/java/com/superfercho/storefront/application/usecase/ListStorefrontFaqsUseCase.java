package com.superfercho.storefront.application.usecase;

import com.superfercho.storefront.application.port.StorefrontFaqRepository;
import com.superfercho.storefront.domain.model.StorefrontFaq;
import java.util.List;

/** Consulta administrativa: todas las preguntas, incluidas las inactivas. */
public final class ListStorefrontFaqsUseCase {

    private final StorefrontFaqRepository faqRepository;

    public ListStorefrontFaqsUseCase(StorefrontFaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    public List<StorefrontFaq> execute() {
        return faqRepository.findAllOrdered();
    }
}
