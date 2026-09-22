package com.superfercho.identity.infrastructure.configuration;

import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalDevAdminRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalDevAdminRunner.class);

    static final String DOCUMENT_TYPE = "CC";
    static final String FULL_NAME = "Administrador local";
    static final String PHONE = "3000000000";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;
    private final String configuredEmail;
    private final String configuredPassword;

    public LocalDevAdminRunner(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            Clock clock,
            @Value("${superfercho.dev.admin.email:}") String configuredEmail,
            @Value("${superfercho.dev.admin.password:}") String configuredPassword) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
        this.configuredEmail = configuredEmail;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureAdmin(configuredEmail, configuredPassword);
    }

    void ensureAdmin(String rawEmail, String rawPassword) {
        if (isBlank(rawEmail) || isBlank(rawPassword)) {
            LOGGER.info(
                    "Local development ADMIN bootstrap skipped: SUPERFERCHO_DEV_ADMIN_EMAIL and SUPERFERCHO_DEV_ADMIN_PASSWORD must both be set.");
            return;
        }

        String email = User.normalizeEmail(rawEmail);
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            updateExisting(existing.get(), rawPassword);
            return;
        }

        createAdmin(email, rawPassword);
    }

    private void updateExisting(User existing, String rawPassword) {
        boolean passwordMatches = passwordHasher.matches(rawPassword, existing.passwordHash());
        boolean alreadyConfigured =
                existing.role() == Role.ADMIN && existing.status() == UserStatus.ACTIVE && passwordMatches;
        if (alreadyConfigured) {
            LOGGER.info("Local development ADMIN already configured for {}", existing.email());
            return;
        }

        String passwordHash =
                passwordMatches ? existing.passwordHash() : passwordHasher.hash(rawPassword);
        User updated = User.create(
                existing.id(),
                existing.documentType(),
                existing.documentNumber(),
                existing.fullName(),
                existing.email(),
                existing.phone(),
                passwordHash,
                Role.ADMIN,
                UserStatus.ACTIVE,
                existing.createdAt(),
                clock.instant());
        userRepository.save(updated);
        LOGGER.info("Updated local development ADMIN for {}", updated.email());
    }

    private void createAdmin(String email, String rawPassword) {
        Instant now = clock.instant();
        UUID id = UUID.randomUUID();
        User created = User.create(
                id,
                DOCUMENT_TYPE,
                documentNumberFor(id),
                FULL_NAME,
                email,
                PHONE,
                passwordHasher.hash(rawPassword),
                Role.ADMIN,
                UserStatus.ACTIVE,
                now,
                now);
        userRepository.save(created);
        LOGGER.info("Created local development ADMIN for {}", created.email());
    }

    private static String documentNumberFor(UUID id) {
        return id.toString().replace("-", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
