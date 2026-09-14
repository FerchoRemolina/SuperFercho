package com.superfercho.identity.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.identity.domain.exception.InvalidUserException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void shouldCreateValidUser() {
        User user = validUser().email("Ada@Example.com").build();

        assertEquals(ID, user.id());
        assertEquals("CC", user.documentType());
        assertEquals("12345678", user.documentNumber());
        assertEquals("Ada Lovelace", user.fullName());
        assertEquals("ada@example.com", user.email());
        assertEquals("3001234567", user.phone());
        assertEquals("hashed-password", user.passwordHash());
        assertEquals(Role.CUSTOMER, user.role());
        assertEquals(UserStatus.ACTIVE, user.status());
        assertEquals(CREATED_AT, user.createdAt());
        assertEquals(UPDATED_AT, user.updatedAt());
    }

    @Test
    void shouldRejectUserWhenIdIsNull() {
        assertThrows(InvalidUserException.class, () -> validUser().id(null).build());
    }

    @Test
    void shouldRejectUserWhenDocumentTypeIsBlank() {
        assertThrows(InvalidUserException.class, () -> validUser().documentType("  ").build());
    }

    @Test
    void shouldRejectUserWhenDocumentNumberIsBlank() {
        assertThrows(InvalidUserException.class, () -> validUser().documentNumber("").build());
    }

    @Test
    void shouldRejectUserWhenFullNameIsBlank() {
        assertThrows(InvalidUserException.class, () -> validUser().fullName(" ").build());
    }

    @Test
    void shouldRejectUserWhenEmailIsBlank() {
        assertThrows(InvalidUserException.class, () -> validUser().email("   ").build());
    }

    @Test
    void shouldNormalizeEmailWhenCreatingUser() {
        User user = validUser().email("  Ada.Lovelace@Example.COM  ").build();

        assertEquals("ada.lovelace@example.com", user.email());
    }

    @Test
    void shouldRejectUserWhenPhoneIsBlank() {
        assertThrows(InvalidUserException.class, () -> validUser().phone(null).build());
    }

    @Test
    void shouldRejectUserWhenPasswordHashIsBlank() {
        assertThrows(InvalidUserException.class, () -> validUser().passwordHash("").build());
    }

    @Test
    void shouldRejectUserWhenRoleIsNull() {
        assertThrows(InvalidUserException.class, () -> validUser().role(null).build());
    }

    @Test
    void shouldRejectUserWhenStatusIsNull() {
        assertThrows(InvalidUserException.class, () -> validUser().status(null).build());
    }

    @Test
    void shouldRejectUserWhenCreatedAtIsAfterUpdatedAt() {
        assertThrows(
                InvalidUserException.class,
                () -> validUser()
                        .createdAt(Instant.parse("2026-01-02T00:00:00Z"))
                        .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                        .build());
    }

    private static UserBuilder validUser() {
        return new UserBuilder();
    }

    private static final class UserBuilder {
        private UUID id = ID;
        private String documentType = "CC";
        private String documentNumber = "12345678";
        private String fullName = "Ada Lovelace";
        private String email = "ada@example.com";
        private String phone = "3001234567";
        private String passwordHash = "hashed-password";
        private Role role = Role.CUSTOMER;
        private UserStatus status = UserStatus.ACTIVE;
        private Instant createdAt = CREATED_AT;
        private Instant updatedAt = UPDATED_AT;

        private UserBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        private UserBuilder documentType(String documentType) {
            this.documentType = documentType;
            return this;
        }

        private UserBuilder documentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
            return this;
        }

        private UserBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        private UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        private UserBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        private UserBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        private UserBuilder role(Role role) {
            this.role = role;
            return this;
        }

        private UserBuilder status(UserStatus status) {
            this.status = status;
            return this;
        }

        private UserBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        private UserBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        private User build() {
            return User.create(
                    id,
                    documentType,
                    documentNumber,
                    fullName,
                    email,
                    phone,
                    passwordHash,
                    role,
                    status,
                    createdAt,
                    updatedAt);
        }
    }
}
