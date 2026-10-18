package com.superfercho.storefront.application.usecase;

import com.superfercho.storefront.application.dto.UpdateStorefrontFaqCommand;
import com.superfercho.storefront.application.port.ClockPort;
import com.superfercho.storefront.application.port.StorefrontFaqRepository;
import com.superfercho.storefront.application.exception.StorefrontContentNotFoundException;
import com.superfercho.storefront.domain.model.StorefrontFaq;
import java.util.Objects;
import java.util.UUID;

/** Edición de una pregunta frecuente existente (ADMIN). */
public final class UpdateStorefrontFaqUseCase {

    private final StorefrontFaqRepository faqRepository;
    private final ClockPort clockPort;

    public UpdateStorefrontFaqUseCase(
            StorefrontFaqRepository faqRepository, ClockPort clockPort) {
        this.faqRepository = Objects.requireNonNull(faqRepository, "faqRepository");
        this.clockPort = Objects.requireNonNull(clockPort, "clockPort");
    }

    public StorefrontFaq execute(UpdateStorefrontFaqCommand command) {
        Objects.requireNonNull(command, "command");
        StorefrontFaq current = faqRepository
                .findById(command.id())
                .orElseThrow(() -> new StorefrontContentNotFoundException(
                        "la pregunta frecuente no existe: " + command.id()));
        StorefrontFaq updated = current
                .withContent(command.question(), command.answer())
                .withDisplayOrder(command.displayOrder())
                .withActive(command.active());
        // updatedAt refleja la edición en la persistencia (el adaptador usa el
        // updatedAt del dominio conservando createdAt).
        return faqRepository.save(new StorefrontFaq(
                updated.id(),
                updated.question(),
                updated.answer(),
                updated.displayOrder(),
                updated.active(),
                updated.createdAt(),
                clockPort.currentTime()));
    }
}
