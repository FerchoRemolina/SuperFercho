package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.StorefrontPreviewStatusResult;
import com.superfercho.identity.domain.model.CustomerPreviewStatus;
import java.time.Instant;
import java.util.UUID;

public record StorefrontPreviewStatusRestResponse(
        UUID previewId,
        UUID adminUserId,
        UUID temporaryCustomerId,
        CustomerPreviewStatus status,
        Instant previewCreatedAt,
        Instant previewExpiresAt,
        long remainingSeconds,
        boolean usable) {

    public static StorefrontPreviewStatusRestResponse from(StorefrontPreviewStatusResult result) {
        return new StorefrontPreviewStatusRestResponse(
                result.previewId(),
                result.adminUserId(),
                result.temporaryCustomerId(),
                result.status(),
                result.previewCreatedAt(),
                result.previewExpiresAt(),
                result.remainingSeconds(),
                result.usable());
    }
}
