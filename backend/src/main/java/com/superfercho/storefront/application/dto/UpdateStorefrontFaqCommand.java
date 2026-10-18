package com.superfercho.storefront.application.dto;

import java.util.UUID;

/** Edición de una pregunta frecuente existente (texto, orden y estado). */
public record UpdateStorefrontFaqCommand(
        UUID id,
        String question,
        String answer,
        int displayOrder,
        boolean active) {}
