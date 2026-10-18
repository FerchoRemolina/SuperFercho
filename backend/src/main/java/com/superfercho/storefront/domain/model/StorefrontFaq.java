package com.superfercho.storefront.domain.model;

import com.superfercho.storefront.domain.exception.InvalidStorefrontContentException;
import java.time.Instant;
import java.util.UUID;

/**
 * Pregunta frecuente del storefront. El orden de visualización es un entero
 * >= 0 configurable por el Admin; las consultas públicas solo reciben las
 * activas ordenadas por {@code displayOrder}.
 */
public record StorefrontFaq(
        UUID id,
        String question,
        String answer,
        int displayOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static final int QUESTION_MAX_LENGTH = 300;
    public static final int ANSWER_MAX_LENGTH = 2000;

    public StorefrontFaq {
        if (id == null) {
            throw new InvalidStorefrontContentException("faq id es obligatorio");
        }
        question = requireText(question, "question", QUESTION_MAX_LENGTH);
        answer = requireText(answer, "answer", ANSWER_MAX_LENGTH);
        if (displayOrder < 0) {
            throw new InvalidStorefrontContentException(
                    "displayOrder no puede ser negativo");
        }
        if (createdAt == null || updatedAt == null) {
            throw new InvalidStorefrontContentException(
                    "las fechas de la FAQ son obligatorias");
        }
    }

    public static StorefrontFaq create(
            UUID id,
            String question,
            String answer,
            int displayOrder,
            boolean active,
            Instant now) {
        return new StorefrontFaq(id, question, answer, displayOrder, active, now, now);
    }

    public StorefrontFaq withContent(String newQuestion, String newAnswer) {
        return new StorefrontFaq(id, newQuestion, newAnswer, displayOrder, active, createdAt, updatedAt);
    }

    public StorefrontFaq withDisplayOrder(int newDisplayOrder) {
        return new StorefrontFaq(id, question, answer, newDisplayOrder, active, createdAt, updatedAt);
    }

    public StorefrontFaq withActive(boolean newActive) {
        return new StorefrontFaq(id, question, answer, displayOrder, newActive, createdAt, updatedAt);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidStorefrontContentException(field + " es obligatorio");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new InvalidStorefrontContentException(
                    field + " no puede exceder " + maxLength + " caracteres");
        }
        return trimmed;
    }
}
