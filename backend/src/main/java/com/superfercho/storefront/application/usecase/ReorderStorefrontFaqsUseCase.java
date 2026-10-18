package com.superfercho.storefront.application.usecase;

import com.superfercho.storefront.application.dto.ReorderStorefrontFaqsCommand;
import com.superfercho.storefront.application.port.ClockPort;
import com.superfercho.storefront.application.port.StorefrontFaqRepository;
import com.superfercho.storefront.domain.exception.InvalidStorefrontContentException;
import com.superfercho.storefront.domain.model.StorefrontFaq;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Reordenación en bloque (ADMIN): asigna display_order 0..n-1 según el orden
 * de los ids recibidos. La lista debe contener todas las preguntas existentes
 * sin duplicados.
 */
public final class ReorderStorefrontFaqsUseCase {

    private final StorefrontFaqRepository faqRepository;
    private final ClockPort clockPort;

    public ReorderStorefrontFaqsUseCase(
            StorefrontFaqRepository faqRepository, ClockPort clockPort) {
        this.faqRepository = Objects.requireNonNull(faqRepository, "faqRepository");
        this.clockPort = Objects.requireNonNull(clockPort, "clockPort");
    }

    public List<StorefrontFaq> execute(ReorderStorefrontFaqsCommand command) {
        Objects.requireNonNull(command, "command");
        List<UUID> orderedIds = Objects.requireNonNull(command.orderedIds(), "orderedIds");
        Set<UUID> unique = new HashSet<>(orderedIds);
        if (unique.size() != orderedIds.size()) {
            throw new InvalidStorefrontContentException("orderedIds contiene duplicados");
        }
        List<StorefrontFaq> existing = faqRepository.findAllOrdered();
        if (existing.size() != orderedIds.size()) {
            throw new InvalidStorefrontContentException(
                    "orderedIds debe contener todas las preguntas existentes");
        }
        return orderedIds.stream()
                .map(id -> {
                    StorefrontFaq faq = faqRepository
                            .findById(id)
                            .orElseThrow(() -> new InvalidStorefrontContentException(
                                    "la pregunta no existe: " + id));
                    StorefrontFaq reordered = faq.withDisplayOrder(orderedIds.indexOf(id));
                    return faqRepository.save(new StorefrontFaq(
                            reordered.id(),
                            reordered.question(),
                            reordered.answer(),
                            reordered.displayOrder(),
                            reordered.active(),
                            reordered.createdAt(),
                            clockPort.currentTime()));
                })
                .toList();
    }
}
