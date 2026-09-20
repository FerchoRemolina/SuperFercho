package com.superfercho.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.domain.model.Role;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SpringSecurityCurrentUserProviderTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final CurrentUserProvider currentUserProvider = new SpringSecurityCurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnAuthenticatedUserIdFromSecurityContext() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(USER_ID, Role.CUSTOMER);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        assertEquals(USER_ID, currentUserProvider.getCurrentUserId());
    }

    @Test
    void shouldRejectWhenSecurityContextIsEmpty() {
        assertThrows(UnauthenticatedUserException.class, currentUserProvider::getCurrentUserId);
    }

    @Test
    void shouldRejectAnonymousAuthentication() {
        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken(
                        "anonymous",
                        "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThrows(UnauthenticatedUserException.class, currentUserProvider::getCurrentUserId);
    }
}
