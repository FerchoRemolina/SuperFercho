package com.superfercho.identity.application.dto;

import com.superfercho.identity.domain.model.CustomerPreviewStatus;
import com.superfercho.identity.domain.model.Role;
import java.time.Instant;
import java.util.UUID;

public record StorefrontPreviewSessionResult(
        UUID previewId,
        UUID adminUserId,
        UUID temporaryCustomerId,
        Role temporaryCustomerRole,
        CustomerPreviewStatus status,
        Instant previewCreatedAt,
        Instant previewExpiresAt,
        long remainingSeconds,
        String accessToken,
        Instant accessTokenExpiresAt) {}
