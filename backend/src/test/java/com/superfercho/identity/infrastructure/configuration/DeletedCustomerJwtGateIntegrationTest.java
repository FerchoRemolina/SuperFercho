package com.superfercho.identity.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.identity.application.dto.AuthenticateUserCommand;
import com.superfercho.identity.application.dto.AuthenticationResult;
import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.usecase.AuthenticateUserUseCase;
import com.superfercho.identity.application.usecase.CloseCustomerAccountUseCase;
import com.superfercho.identity.application.usecase.RegisterCustomerUseCase;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.identity.infrastructure.security.AuthenticatedUserPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class DeletedCustomerJwtGateIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

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
    private MockMvc mockMvc;

    @Autowired
    private RegisterCustomerUseCase registerCustomerUseCase;

    @Autowired
    private AuthenticateUserUseCase authenticateUserUseCase;

    @Autowired
    private CloseCustomerAccountUseCase closeCustomerAccountUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRecordRepository customerRecordRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void closedCustomerJwtIsRejectedWhileHistoryRemains() throws Exception {
        RegisteredCustomer registered = registerCustomerUseCase.execute(new RegisterCustomerCommand(
                "CC",
                "66778899",
                "Ada",
                "Lovelace",
                "jwt-gate@example.com",
                "3001234567",
                "Luis123!"));
        UUID customerId = registered.id();
        UUID recordId = userRepository.findById(customerId).orElseThrow().customerRecordId();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        seedOrderAndPayment(customerId, orderId, paymentId);

        AuthenticationResult auth = authenticateUserUseCase.execute(
                new AuthenticateUserCommand("jwt-gate@example.com", "Luis123!"));
        String jwt = auth.accessToken();

        mockMvc.perform(get("/api/v1/addresses").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk());

        authenticateAs(customerId);
        closeCustomerAccountUseCase.execute();
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/addresses").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        User closed = userRepository.findById(customerId).orElseThrow();
        assertThat(closed.status()).isEqualTo(UserStatus.INACTIVE);
        assertThat(closed.deletedAt()).isNotNull();
        assertThat(closed.customerRecordId()).isEqualTo(recordId);
        assertThat(customerRecordRepository.findById(recordId)).isPresent();
        assertThat(count("select count(*) from orders.orders where id = ?", orderId)).isEqualTo(1);
        assertThat(count("select count(*) from payments.payments where id = ?", paymentId)).isEqualTo(1);
    }

    private void authenticateAs(UUID userId) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(userId, Role.CUSTOMER);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private void seedOrderAndPayment(UUID customerId, UUID orderId, UUID paymentId) {
        jdbcTemplate.update(
                """
                insert into orders.orders (
                    id, order_number, customer_id, status,
                    subtotal_amount, subtotal_currency, total_amount, total_currency, payment_id,
                    shipping_recipient_name, shipping_address_line, shipping_additional_info,
                    shipping_city, shipping_department, shipping_phone,
                    created_at, confirmed_at, cancelled_at, updated_at)
                values (?, ?, ?, 'CONFIRMED', ?, 'COP', ?, 'COP', ?,
                        'Ada', 'Calle 1', null, 'Bogotá', 'Cundinamarca', '3001234567',
                        ?, ?, null, ?)
                """,
                orderId,
                "ORD-" + orderId.toString().substring(0, 8),
                customerId,
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                paymentId,
                java.sql.Timestamp.from(NOW),
                java.sql.Timestamp.from(NOW),
                java.sql.Timestamp.from(NOW));
        jdbcTemplate.update(
                """
                insert into payments.payments (
                    id, order_id, amount, currency, payment_method, status,
                    provider_reference, created_at, updated_at, refunded_at)
                values (?, ?, ?, 'COP', 'CASH_ON_DELIVERY', 'APPROVED', null, ?, ?, null)
                """,
                paymentId,
                orderId,
                new BigDecimal("10.00"),
                java.sql.Timestamp.from(NOW),
                java.sql.Timestamp.from(NOW));
    }

    private int count(String sql, UUID id) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return value == null ? 0 : value;
    }
}
