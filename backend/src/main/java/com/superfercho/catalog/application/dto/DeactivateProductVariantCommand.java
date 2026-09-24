package com.superfercho.catalog.application.dto;

import java.util.UUID;

public record DeactivateProductVariantCommand(UUID productVariantId) {
}
