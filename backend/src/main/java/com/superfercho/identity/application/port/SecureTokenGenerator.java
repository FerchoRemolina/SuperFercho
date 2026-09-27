package com.superfercho.identity.application.port;

/** Generates cryptographically secure opaque tokens (never JWTs). */
public interface SecureTokenGenerator {

    String generate();

    String hash(String rawToken);
}
