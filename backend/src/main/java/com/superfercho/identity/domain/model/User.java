package com.superfercho.identity.domain.model;

import com.superfercho.identity.domain.exception.InvalidUserException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class User {

    private final UUID id;
    private final String documentType;
    private final String documentNumber;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final String passwordHash;
    private final Role role;
    private final UserStatus status;
    private final UUID customerRecordId;
    private final Instant deletedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(
            UUID id,
            String documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String passwordHash,
            Role role,
            UserStatus status,
            UUID customerRecordId,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.customerRecordId = customerRecordId;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(
            UUID id,
            String documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String passwordHash,
            Role role,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt) {
        return create(
                id,
                documentType,
                documentNumber,
                firstName,
                lastName,
                email,
                phone,
                passwordHash,
                role,
                status,
                null,
                null,
                createdAt,
                updatedAt);
    }

    public static User create(
            UUID id,
            String documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String passwordHash,
            Role role,
            UserStatus status,
            UUID customerRecordId,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt) {
        requireNonNull(id, "id");
        requireText(documentType, "documentType");
        requireText(documentNumber, "documentNumber");
        requireText(firstName, "firstName");
        requireNonNull(lastName, "lastName");
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
                firstName,
                lastName,
                normalizeEmail(email),
                phone,
                passwordHash,
                role,
                status,
                customerRecordId,
                deletedAt,
                createdAt,
                updatedAt);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Soft-closes the operational CUSTOMER account. Idempotent when already closed. Does not remove
     * the commercial {@code customerRecordId} link or mutate historical identity fields.
     */
    public User closeAccount(Instant closedAt) {
        requireNonNull(closedAt, "closedAt");
        if (role != Role.CUSTOMER) {
            throw new InvalidUserException("Only CUSTOMER accounts can be closed");
        }
        if (customerRecordId == null) {
            throw new InvalidUserException("CUSTOMER account without CustomerRecord cannot be closed");
        }
        if (deletedAt != null) {
            return this;
        }
        return create(
                id,
                documentType,
                documentNumber,
                firstName,
                lastName,
                email,
                phone,
                passwordHash,
                role,
                UserStatus.INACTIVE,
                customerRecordId,
                closedAt,
                createdAt,
                closedAt);
    }

    /** Visible Customer name (first name). */
    public String displayFirstName() {
        return firstName;
    }

    /** Full display when both parts exist: "Luis Remolina". */
    public String displayFullName() {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName;
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

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
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

    public UUID customerRecordId() {
        return customerRecordId;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
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
