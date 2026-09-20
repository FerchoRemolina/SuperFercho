package com.superfercho.identity.infrastructure.security;

import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import com.superfercho.identity.application.port.CurrentUserProvider;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthenticatedUserException();
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return principal.userId();
        }
        throw new UnauthenticatedUserException();
    }
}
