package com.superfercho.identity.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.usecase.RegisterCustomerUseCase;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RegisterCustomerConcurrencyIntegrationTest {

    private static final String DOCUMENT_TYPE = "CC";
    private static final String DOCUMENT_NUMBER = "55112233";

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
        registry.add("spring.task.scheduling.enabled", () -> "false");
        registry.add(
                "superfercho.security.jwt.secret", () -> "test-only-superfercho-jwt-secret-key-32b");
    }

    @Autowired
    private RegisterCustomerUseCase registerCustomerUseCase;

    @Autowired
    private CustomerRecordRepository customerRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentRegistrationForSameDocumentCreatesOneCustomerRecordAndOneLiveUser()
            throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger documentConflicts = new AtomicInteger();
        List<Throwable> unexpected = new ArrayList<>();
        List<UUID> registeredIds = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> race(
                    start,
                    ready,
                    successes,
                    documentConflicts,
                    unexpected,
                    registeredIds,
                    "concurrent-a@example.com",
                    "3001111111"));
            Future<?> second = pool.submit(() -> race(
                    start,
                    ready,
                    successes,
                    documentConflicts,
                    unexpected,
                    registeredIds,
                    "concurrent-b@example.com",
                    "3002222222"));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(unexpected).isEmpty();
        assertThat(successes.get()).isEqualTo(1);
        assertThat(documentConflicts.get()).isEqualTo(1);

        CustomerRecord record = customerRecordRepository
                .findByDocument(DOCUMENT_TYPE, DOCUMENT_NUMBER)
                .orElseThrow();
        assertThat(countCustomerRecords(DOCUMENT_TYPE, DOCUMENT_NUMBER)).isEqualTo(1);
        assertThat(countLiveUsersForRecord(record.id())).isEqualTo(1);

        User live = userRepository.findLiveByCustomerRecordId(record.id()).orElseThrow();
        assertThat(registeredIds).containsExactly(live.id());
        assertThat(live.deletedAt()).isNull();
    }

    private void race(
            CountDownLatch start,
            CountDownLatch ready,
            AtomicInteger successes,
            AtomicInteger documentConflicts,
            List<Throwable> unexpected,
            List<UUID> registeredIds,
            String email,
            String phone) {
        ready.countDown();
        try {
            assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            RegisteredCustomer registered = registerCustomerUseCase.execute(new RegisterCustomerCommand(
                    DOCUMENT_TYPE,
                    DOCUMENT_NUMBER,
                    "Ada",
                    "Lovelace",
                    email,
                    phone,
                    "Luis123!"));
            successes.incrementAndGet();
            synchronized (registeredIds) {
                registeredIds.add(registered.id());
            }
        } catch (DocumentAlreadyExistsException exception) {
            documentConflicts.incrementAndGet();
        } catch (Throwable throwable) {
            synchronized (unexpected) {
                unexpected.add(throwable);
            }
        }
    }

    private int countCustomerRecords(String documentType, String documentNumber) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*) from identity.customer_records
                 where document_type = ? and document_number = ?
                """,
                Integer.class,
                documentType,
                documentNumber);
        return count == null ? 0 : count;
    }

    private int countLiveUsersForRecord(UUID customerRecordId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*) from identity.users
                 where customer_record_id = ? and deleted_at is null
                """,
                Integer.class,
                customerRecordId);
        return count == null ? 0 : count;
    }
}
