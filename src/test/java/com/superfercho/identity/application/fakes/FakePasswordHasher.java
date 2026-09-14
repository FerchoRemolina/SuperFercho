package com.superfercho.identity.application.fakes;

import com.superfercho.identity.application.port.PasswordHasher;

public final class FakePasswordHasher implements PasswordHasher {

    private String lastRawPassword;

    @Override
    public String hash(String rawPassword) {
        lastRawPassword = rawPassword;
        return "hashed:" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordHash.equals("hashed:" + rawPassword);
    }

    public String lastRawPassword() {
        return lastRawPassword;
    }
}
