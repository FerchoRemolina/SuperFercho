package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.AuthenticationResult;
import com.superfercho.identity.domain.model.Role;
import java.time.Instant;
import java.util.UUID;

public record AuthenticationRestResponse(UUID userId, Role role, String accessToken, Instant expiresAt) {

    public static AuthenticationRestResponse from(AuthenticationResult result) {
        return new AuthenticationRestResponse(result.userId(), result.role(), result.accessToken(), result.expiresAt());
    }
}
