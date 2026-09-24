package com.superfercho.identity.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.superfercho.identity.application.port.AccessTokenIssuer;
import com.superfercho.identity.application.port.IssuedAccessToken;
import com.superfercho.identity.domain.model.Role;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public final class JwtAccessTokenService implements AccessTokenIssuer {

    static final String ROLE_CLAIM = "role";
    static final String PREVIEW_ID_CLAIM = "previewId";

    private final JwtProperties properties;
    private final Clock clock;
    private final byte[] secret;

    public JwtAccessTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public IssuedAccessToken issue(UUID userId, Role role) {
        return issue(userId, role, null);
    }

    @Override
    public IssuedAccessToken issue(UUID userId, Role role, UUID previewId) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.expiration());
        try {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim(ROLE_CLAIM, role.name())
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt));
            if (previewId != null) {
                claims.claim(PREVIEW_ID_CLAIM, previewId.toString());
            }
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
            jwt.sign(new MACSigner(secret));
            return new IssuedAccessToken(jwt.serialize(), expiresAt);
        } catch (JOSEException ex) {
            throw new IllegalStateException("Failed to issue access token", ex);
        }
    }

    public Optional<AuthenticatedUserPrincipal> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secret))) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Instant expiresAt = claims.getExpirationTime() == null
                    ? Instant.MIN
                    : claims.getExpirationTime().toInstant();
            if (!expiresAt.isAfter(clock.instant())) {
                return Optional.empty();
            }
            UUID userId = UUID.fromString(claims.getSubject());
            Role role = Role.valueOf(claims.getStringClaim(ROLE_CLAIM));
            String previewClaim = claims.getStringClaim(PREVIEW_ID_CLAIM);
            UUID previewId = previewClaim == null || previewClaim.isBlank()
                    ? null
                    : UUID.fromString(previewClaim);
            return Optional.of(new AuthenticatedUserPrincipal(userId, role, previewId));
        } catch (ParseException | JOSEException | IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }
}
