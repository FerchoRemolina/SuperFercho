package com.superfercho.identity.infrastructure.security;

import com.superfercho.identity.domain.model.Role;
import java.util.UUID;

public record AuthenticatedUserPrincipal(UUID userId, Role role) {
}
