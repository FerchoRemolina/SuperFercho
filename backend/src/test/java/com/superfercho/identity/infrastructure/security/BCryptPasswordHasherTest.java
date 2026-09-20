package com.superfercho.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher(new BCryptPasswordEncoder());

    @Test
    void shouldMatchHashedPasswordWhenRawPasswordIsCorrect() {
        String hash = hasher.hash("secret-pass");

        assertTrue(hasher.matches("secret-pass", hash));
    }

    @Test
    void shouldRejectHashedPasswordWhenRawPasswordIsIncorrect() {
        String hash = hasher.hash("secret-pass");

        assertFalse(hasher.matches("other-pass", hash));
    }

    @Test
    void shouldNotStoreRawPasswordAsHash() {
        String hash = hasher.hash("secret-pass");

        assertNotEquals("secret-pass", hash);
        assertFalse(hash.contains("secret-pass"));
    }

    @Test
    void shouldGenerateDifferentHashesForTheSamePassword() {
        String first = hasher.hash("secret-pass");
        String second = hasher.hash("secret-pass");

        assertNotEquals(first, second);
        assertTrue(hasher.matches("secret-pass", first));
        assertTrue(hasher.matches("secret-pass", second));
    }
}
