package com.superfercho.identity.application.port;

import java.time.Instant;

public record IssuedAccessToken(String token, Instant expiresAt) {
}
