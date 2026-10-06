package com.superfercho.assistant.infrastructure.conversation;

import com.superfercho.assistant.application.port.out.VisitorTokenHasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha256VisitorTokenHasher implements VisitorTokenHasher {

    @Override
    public String hash(String rawVisitorToken) {
        if (rawVisitorToken == null || rawVisitorToken.isBlank()) {
            throw new IllegalArgumentException("visitor token cannot be blank");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawVisitorToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
