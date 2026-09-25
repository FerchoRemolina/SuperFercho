package com.superfercho.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.identity.application.port.IssuedAccessToken;
import com.superfercho.identity.domain.model.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtAccessTokenServiceTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-01-15T12:00:00Z");
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String SECRET = "test-only-superfercho-jwt-secret-key-32b";

    @Test
    void shouldIssueTokenWithSubjectRoleAndExpiration() {
        JwtAccessTokenService service = service(Clock.fixed(ISSUED_AT, ZoneOffset.UTC));

        IssuedAccessToken issued = service.issue(USER_ID, Role.CUSTOMER);

        AuthenticatedUserPrincipal principal = service.parse(issued.token()).orElseThrow();
        assertEquals(USER_ID, principal.userId());
        assertEquals(Role.CUSTOMER, principal.role());
        assertEquals(ISSUED_AT.plus(Duration.ofMinutes(20)), issued.expiresAt());
    }

    @Test
    void shouldParseAdminRoleFromIssuedToken() {
        JwtAccessTokenService service = service(Clock.fixed(ISSUED_AT, ZoneOffset.UTC));

        IssuedAccessToken issued = service.issue(USER_ID, Role.ADMIN);

        assertEquals(Role.ADMIN, service.parse(issued.token()).orElseThrow().role());
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtAccessTokenService issuer = service(Clock.fixed(ISSUED_AT, ZoneOffset.UTC));
        IssuedAccessToken issued = issuer.issue(USER_ID, Role.CUSTOMER);

        JwtAccessTokenService parser =
                service(Clock.fixed(ISSUED_AT.plus(Duration.ofMinutes(21)), ZoneOffset.UTC));

        assertTrue(parser.parse(issued.token()).isEmpty());
    }

    @Test
    void shouldRejectInvalidToken() {
        JwtAccessTokenService service = service(Clock.fixed(ISSUED_AT, ZoneOffset.UTC));

        assertTrue(service.parse("not-a-jwt").isEmpty());
        assertTrue(service.parse("").isEmpty());
        assertTrue(service.parse(null).isEmpty());
    }

    @Test
    void shouldRejectTokenSignedWithADifferentSecret() {
        JwtAccessTokenService issuer = service(Clock.fixed(ISSUED_AT, ZoneOffset.UTC));
        IssuedAccessToken issued = issuer.issue(USER_ID, Role.CUSTOMER);

        JwtAccessTokenService otherSecret = new JwtAccessTokenService(
                new JwtProperties("other-only-superfercho-jwt-secret-key-32b", Duration.ofMinutes(20)),
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC));

        assertTrue(otherSecret.parse(issued.token()).isEmpty());
    }

    @Test
    void shouldIssueAndParsePreviewIdClaimWithoutBreakingNormalTokens() {
        JwtAccessTokenService service = service(Clock.fixed(ISSUED_AT, ZoneOffset.UTC));
        UUID previewId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        IssuedAccessToken previewToken = service.issue(USER_ID, Role.CUSTOMER, previewId);
        AuthenticatedUserPrincipal previewPrincipal = service.parse(previewToken.token()).orElseThrow();
        assertEquals(previewId, previewPrincipal.previewId());
        assertTrue(previewPrincipal.isStorefrontPreview());

        IssuedAccessToken normalToken = service.issue(USER_ID, Role.CUSTOMER);
        AuthenticatedUserPrincipal normalPrincipal = service.parse(normalToken.token()).orElseThrow();
        assertEquals(null, normalPrincipal.previewId());
        assertTrue(!normalPrincipal.isStorefrontPreview());
    }

    private static JwtAccessTokenService service(Clock clock) {
        return new JwtAccessTokenService(new JwtProperties(SECRET, Duration.ofMinutes(20)), clock);
    }
}
