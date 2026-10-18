package com.superfercho.storefront.application.usecase;

import com.superfercho.storefront.application.dto.CreateStorefrontFaqCommand;
import com.superfercho.storefront.application.port.ClockPort;
import com.superfercho.storefront.application.port.StorefrontFaqRepository;
import com.superfercho.storefront.domain.model.StorefrontFaq;
import java.util.Objects;
import java.util.UUID;

/** Creación de una pregunta frecuente nueva (ADMIN). */
public final class CreateStorefrontFaqUseCase {

    private final StorefrontFaqRepository faqRepository;
    private final ClockPort clockPort;

    public CreateStorefrontFaqUseCase(
            StorefrontFaqRepository faqRepository, ClockPort clockPort) {
        this.faqRepository = Objects.requireNonNull(faqRepository, "faqRepository");
        this.clockPort = Objects.requireNonNull(clockPort, "clockPort");
    }

    public StorefrontFaq execute(CreateStorefrontFaqCommand command) {
        Objects.requireNonNull(command, "command");
        StorefrontFaq faq = StorefrontFaq.create(
                UUID.randomUUID(),
                command.question(),
                command.answer(),
                command.displayOrder(),
                command.active(),
                clockPort.currentTime());
        return faqRepository.save(faq);
    }
}
