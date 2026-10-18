package com.superfercho.storefront.application.usecase;

import com.superfercho.storefront.application.port.StorefrontFaqRepository;
import com.superfercho.storefront.domain.model.StorefrontFaq;
import java.util.List;

/** Consulta pública: solo preguntas activas, ordenadas para visualización. */
public final class ListActiveStorefrontFaqsUseCase {

    private final StorefrontFaqRepository faqRepository;

    public ListActiveStorefrontFaqsUseCase(StorefrontFaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    public List<StorefrontFaq> execute() {
        return faqRepository.findActiveOrdered();
    }
}
