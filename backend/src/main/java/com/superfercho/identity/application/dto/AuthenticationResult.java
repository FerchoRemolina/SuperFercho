package com.superfercho.identity.application.dto;

import com.superfercho.identity.domain.model.Role;
import java.time.Instant;
import java.util.UUID;

public record AuthenticationResult(UUID userId, Role role, String accessToken, Instant expiresAt) {
}
