package com.superfercho.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class IdentityPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                            DockerImageName.parse("pgvector/pgvector:pg16")
                                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("superfercho")
                    .withUsername("superfercho")
                    .withPassword("superfercho");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Test
    void shouldPersistAndReloadUser() {
        User saved = userRepository.save(newUser("persist@example.com", "CC", "1001"));

        User loaded = userRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.email()).isEqualTo("persist@example.com");
        assertThat(loaded.documentType()).isEqualTo("CC");
        assertThat(loaded.documentNumber()).isEqualTo("1001");
        assertThat(loaded.role()).isEqualTo(Role.CUSTOMER);
        assertThat(loaded.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldRejectDuplicateEmail() {
        userRepository.save(newUser("dup-email@example.com", "CC", "2001"));

        assertThatThrownBy(() -> userRepository.save(newUser("dup-email@example.com", "CE", "2002")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDuplicateDocumentIdentity() {
        userRepository.save(newUser("doc-one@example.com", "CC", "3001"));

        assertThatThrownBy(() -> userRepository.save(newUser("doc-two@example.com", "CC", "3001")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPersistAndReloadAddress() {
        User user = userRepository.save(newUser("addr@example.com", "CC", "4001"));
        Address saved =
                addressRepository.save(
                        user.id(), newAddress("Casa", true, AddressStatus.ACTIVE));

        Address loaded = addressRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.label()).isEqualTo("Casa");
        assertThat(loaded.isDefault()).isTrue();
        assertThat(loaded.status()).isEqualTo(AddressStatus.ACTIVE);
        assertThat(loaded.additionalInfo()).isNull();
    }

    @Test
    void shouldPreventTwoActiveDefaultAddresses() {
        User user = userRepository.save(newUser("defaults@example.com", "CC", "5001"));
        addressRepository.save(user.id(), newAddress("Casa", true, AddressStatus.ACTIVE));

        assertThatThrownBy(
                        () -> addressRepository.save(
                                user.id(), newAddress("Oficina", true, AddressStatus.ACTIVE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSecondAddressWhenNotActiveDefault() {
        User user = userRepository.save(newUser("two-addr@example.com", "CC", "6001"));
        addressRepository.save(user.id(), newAddress("Casa", true, AddressStatus.ACTIVE));

        Address work =
                addressRepository.save(user.id(), newAddress("Oficina", false, AddressStatus.ACTIVE));

        assertThat(addressRepository.findById(work.id())).isPresent();
        assertThat(work.isDefault()).isFalse();
    }

    private static User newUser(String email, String documentType, String documentNumber) {
        return User.create(
                UUID.randomUUID(),
                documentType,
                documentNumber,
                "Ada Lovelace",
                email,
                "3001234567",
                "hashed-password",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                NOW,
                NOW);
    }

    private static Address newAddress(String label, boolean isDefault, AddressStatus status) {
        return Address.create(
                UUID.randomUUID(),
                label,
                "Ada Lovelace",
                "Calle 1 # 2-3",
                null,
                "Bogotá",
                "Cundinamarca",
                "3001234567",
                isDefault,
                status,
                NOW,
                NOW);
    }
}
