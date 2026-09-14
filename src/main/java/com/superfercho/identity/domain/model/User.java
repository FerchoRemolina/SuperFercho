package com.superfercho.identity.domain.model;

import com.superfercho.identity.domain.exception.InvalidUserException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class User {

    private final UUID id;
    private final String documentType;
    private final String documentNumber;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String passwordHash;
    private final Role role;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(
            UUID id,
            String documentType,
            String documentNumber,
            String fullName,
            String email,
            String phone,
            String passwordHash,
            Role role,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(
            UUID id,
            String documentType,
            String documentNumber,
            String fullName,
            String email,
            String phone,
            String passwordHash,
            Role role,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt) {
        requireNonNull(id, "id");
        requireText(documentType, "documentType");
        requireText(documentNumber, "documentNumber");
        requireText(fullName, "fullName");
        requireText(email, "email");
        requireText(phone, "phone");
        requireText(passwordHash, "passwordHash");
        requireNonNull(role, "role");
        requireNonNull(status, "status");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidUserException("createdAt must not be after updatedAt");
        }

        return new User(
                id,
                documentType,
                documentNumber,
                fullName,
                normalizeEmail(email),
                phone,
                passwordHash,
                role,
                status,
                createdAt,
                updatedAt);
    }

    public UUID id() {
        return id;
    }

    public String documentType() {
        return documentType;
    }

    public String documentNumber() {
        return documentNumber;
    }

    public String fullName() {
        return fullName;
    }

    public String email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public UserStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidUserException(field + " cannot be null");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidUserException(field + " cannot be null or blank");
        }
    }
}
