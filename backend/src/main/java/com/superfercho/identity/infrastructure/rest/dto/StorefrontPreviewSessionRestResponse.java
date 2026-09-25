package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.StorefrontPreviewSessionResult;
import com.superfercho.identity.domain.model.CustomerPreviewStatus;
import com.superfercho.identity.domain.model.Role;
import java.time.Instant;
import java.util.UUID;

public record StorefrontPreviewSessionRestResponse(
        UUID previewId,
        UUID adminUserId,
        UUID temporaryCustomerId,
        Role role,
        CustomerPreviewStatus status,
        Instant previewCreatedAt,
        Instant previewExpiresAt,
        long remainingSeconds,
        String accessToken,
        Instant accessTokenExpiresAt,
        String adminAccessToken,
        Instant adminAccessTokenExpiresAt) {

    public static StorefrontPreviewSessionRestResponse from(StorefrontPreviewSessionResult result) {
        return new StorefrontPreviewSessionRestResponse(
                result.previewId(),
                result.adminUserId(),
                result.temporaryCustomerId(),
                result.temporaryCustomerRole(),
                result.status(),
                result.previewCreatedAt(),
                result.previewExpiresAt(),
                result.remainingSeconds(),
                result.accessToken(),
                result.accessTokenExpiresAt(),
                result.adminAccessToken(),
                result.adminAccessTokenExpiresAt());
    }
}
