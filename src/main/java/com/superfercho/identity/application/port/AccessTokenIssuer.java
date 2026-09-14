package com.superfercho.identity.application.port;

import com.superfercho.identity.domain.model.Role;
import java.util.UUID;

public interface AccessTokenIssuer {

    IssuedAccessToken issue(UUID userId, Role role);
}
