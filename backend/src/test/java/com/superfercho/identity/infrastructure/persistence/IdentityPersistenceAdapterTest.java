package com.superfercho.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.DuplicateDefaultAddressException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.identity.infrastructure.configuration.LocalDevAdminRunner;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
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
        registry.add(
                "superfercho.security.jwt.secret", () -> "test-only-superfercho-jwt-secret-key-32b");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRecordRepository customerRecordRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private LocalDevAdminRunner localDevAdminRunner;

    @Test
    void localDevAdminRunnerIsNotLoadedOutsideLocalProfile() {
        assertThat(localDevAdminRunner).isNull();
    }

    @Test
    void shouldPersistAndReloadUser() {
        User saved = userRepository.save(newUser("persist@example.com", "CC", "1001"));

        User loaded = userRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.email()).isEqualTo("persist@example.com");
        assertThat(loaded.documentType()).isEqualTo("CC");
        assertThat(loaded.documentNumber()).isEqualTo("1001");
        assertThat(loaded.role()).isEqualTo(Role.CUSTOMER);
        assertThat(loaded.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(loaded.customerRecordId()).isNull();
        assertThat(loaded.deletedAt()).isNull();
    }

    @Test
    void shouldPersistAndReloadCustomerRecord() {
        CustomerRecord saved = customerRecordRepository.save(newCustomerRecord("CC", "9001"));

        CustomerRecord loaded = customerRecordRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.documentType()).isEqualTo("CC");
        assertThat(loaded.documentNumber()).isEqualTo("9001");
        assertThat(loaded.billingFirstName()).isEqualTo("Ada");
        assertThat(loaded.billingLastName()).isEqualTo("Lovelace");
    }

    @Test
    void shouldRejectDuplicateCustomerRecordDocument() {
        customerRecordRepository.save(newCustomerRecord("CC", "9002"));

        assertThatThrownBy(() -> customerRecordRepository.save(newCustomerRecord("CC", "9002")))
                .isInstanceOf(DocumentAlreadyExistsException.class);
    }

    @Test
    void shouldAssociateUserWithCustomerRecord() {
        CustomerRecord record = customerRecordRepository.save(newCustomerRecord("CC", "9003"));
        User saved = userRepository.save(newUserWithRecord(
                "linked@example.com", "CC", "9003", Role.CUSTOMER, record.id(), null));

        User loaded = userRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.customerRecordId()).isEqualTo(record.id());
        assertThat(loaded.deletedAt()).isNull();
    }

    @Test
    void shouldAllowNullCustomerRecordIdForAdminAndPreviewStyleUsers() {
        User admin = userRepository.save(newUserWithRecord(
                "admin-null-record@example.com", "CC", "9101", Role.ADMIN, null, null));
        User previewStyle = userRepository.save(newUserWithRecord(
                "preview+temp@temp.superfercho.local",
                "CC",
                "PREV" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                Role.CUSTOMER,
                null,
                null));

        assertThat(userRepository.findById(admin.id()).orElseThrow().customerRecordId()).isNull();
        assertThat(userRepository.findById(previewStyle.id()).orElseThrow().customerRecordId()).isNull();
    }

    @Test
    void shouldRejectDuplicateEmailAmongLiveUsers() {
        userRepository.save(newUser("dup-email@example.com", "CC", "2001"));

        assertThatThrownBy(() -> userRepository.save(newUser("dup-email@example.com", "CE", "2002")))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void shouldAllowSameEmailWhenExistingUserIsDeleted() {
        CustomerRecord record = customerRecordRepository.save(newCustomerRecord("CC", "2003"));
        userRepository.save(newUserWithRecord(
                "reuse-email@example.com",
                "CC",
                "2003",
                Role.CUSTOMER,
                record.id(),
                NOW.plusSeconds(60)));

        User reused = userRepository.save(newUser("reuse-email@example.com", "CE", "2004"));

        assertThat(reused.email()).isEqualTo("reuse-email@example.com");
        assertThat(reused.deletedAt()).isNull();
    }

    @Test
    void shouldAllowOnlyOneLiveUserPerCustomerRecord() {
        CustomerRecord record = customerRecordRepository.save(newCustomerRecord("CC", "2005"));
        userRepository.save(newUserWithRecord(
                "live-one@example.com", "CC", "2005", Role.CUSTOMER, record.id(), null));

        assertThatThrownBy(() -> userRepository.save(newUserWithRecord(
                        "live-two@example.com", "CC", "2005", Role.CUSTOMER, record.id(), null)))
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
                .isInstanceOf(DuplicateDefaultAddressException.class);
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

    @Test
    void shouldFindAddressesByUserId() {
        User owner = userRepository.save(newUser("owner-addr@example.com", "CC", "7001"));
        User other = userRepository.save(newUser("other-addr@example.com", "CC", "7002"));
        Address own = addressRepository.save(owner.id(), newAddress("Casa", true, AddressStatus.ACTIVE));
        addressRepository.save(other.id(), newAddress("Otro", true, AddressStatus.ACTIVE));

        assertThat(addressRepository.findByUserId(owner.id()))
                .extracting(Address::id)
                .containsExactly(own.id());
    }

    @Test
    void v17SchemaReplacesDocumentUniqueWithCustomerRecordConstraints() {
        assertThat(countConstraint("uk_identity_users_document")).isZero();
        assertThat(countIndex("uk_identity_users_email")).isEqualTo(1);
        assertThat(countIndex("uk_identity_users_one_live_customer_record")).isEqualTo(1);
        assertThat(countConstraint("uk_identity_customer_records_document")).isEqualTo(1);
        assertThat(countConstraint("fk_identity_users_customer_record")).isEqualTo(1);
        assertThat(isDocumentColumnNullable("document_type")).isTrue();
        assertThat(isDocumentColumnNullable("document_number")).isTrue();
    }

    private boolean isDocumentColumnNullable(String columnName) {
        Boolean nullable = jdbcTemplate.queryForObject(
                """
                select is_nullable = 'YES'
                  from information_schema.columns
                 where table_schema = 'identity'
                   and table_name = 'users'
                   and column_name = ?
                """,
                Boolean.class,
                columnName);
        return Boolean.TRUE.equals(nullable);
    }

    private int countConstraint(String name) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*) from pg_constraint
                 where conname = ?
                """,
                Integer.class,
                name);
        return count == null ? 0 : count;
    }

    private int countIndex(String name) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*) from pg_class c
                 join pg_namespace n on n.oid = c.relnamespace
                 where c.relkind = 'i'
                   and c.relname = ?
                   and n.nspname = 'identity'
                """,
                Integer.class,
                name);
        return count == null ? 0 : count;
    }

    private static User newUser(String email, String documentType, String documentNumber) {
        return newUserWithRecord(email, documentType, documentNumber, Role.CUSTOMER, null, null);
    }

    private static User newUserWithRecord(
            String email,
            String documentType,
            String documentNumber,
            Role role,
            UUID customerRecordId,
            Instant deletedAt) {
        return User.create(
                UUID.randomUUID(),
                documentType,
                documentNumber,
                "Ada",
                "Lovelace",
                email,
                "3001234567",
                "hashed-password",
                role,
                UserStatus.ACTIVE,
                customerRecordId,
                deletedAt,
                NOW,
                deletedAt == null ? NOW : deletedAt);
    }

    private static CustomerRecord newCustomerRecord(String documentType, String documentNumber) {
        return CustomerRecord.create(
                UUID.randomUUID(),
                documentType,
                documentNumber,
                "Ada",
                "Lovelace",
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
