package com.superfercho.identity.application.dto;

import com.superfercho.identity.domain.model.CustomerPreviewStatus;
import java.time.Instant;
import java.util.UUID;

public record StorefrontPreviewStatusResult(
        UUID previewId,
        UUID adminUserId,
        UUID temporaryCustomerId,
        CustomerPreviewStatus status,
        Instant previewCreatedAt,
        Instant previewExpiresAt,
        long remainingSeconds,
        boolean usable) {}
