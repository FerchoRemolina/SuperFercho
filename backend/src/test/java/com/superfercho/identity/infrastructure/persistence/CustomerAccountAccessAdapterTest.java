package com.superfercho.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerAccountAccessAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

    private InMemoryUserRepository users;
    private CustomerAccountAccessAdapter adapter;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        adapter = new CustomerAccountAccessAdapter(users);
    }

    @Test
    void allowsActiveCustomer() {
        UUID id = UUID.randomUUID();
        users.save(customer(id, UserStatus.ACTIVE, null));

        assertThat(adapter.allowsCustomerAccess(id)).isTrue();
    }

    @Test
    void allowsInactiveButNotDeletedCustomer() {
        UUID id = UUID.randomUUID();
        users.save(customer(id, UserStatus.INACTIVE, null));

        assertThat(adapter.allowsCustomerAccess(id)).isTrue();
    }

    @Test
    void rejectsDeletedCustomer() {
        UUID id = UUID.randomUUID();
        users.save(customer(id, UserStatus.INACTIVE, NOW));

        assertThat(adapter.allowsCustomerAccess(id)).isFalse();
    }

    @Test
    void rejectsMissingUser() {
        assertThat(adapter.allowsCustomerAccess(UUID.randomUUID())).isFalse();
    }

    private static User customer(UUID id, UserStatus status, Instant deletedAt) {
        return User.create(
                id,
                "CC",
                id.toString().substring(0, 8),
                "Ada",
                "Lovelace",
                id + "@example.com",
                "3001234567",
                "hashed",
                Role.CUSTOMER,
                status,
                UUID.randomUUID(),
                deletedAt,
                NOW,
                deletedAt == null ? NOW : deletedAt);
    }
}
