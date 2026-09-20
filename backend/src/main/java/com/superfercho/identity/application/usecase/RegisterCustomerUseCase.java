package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.exception.InvalidUserException;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class RegisterCustomerUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public RegisterCustomerUseCase(
            UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    public RegisteredCustomer execute(RegisterCustomerCommand command) {
        if (command.password() == null || command.password().isBlank()) {
            throw new InvalidRegistrationException("password cannot be null or blank");
        }
        if (command.email() == null || command.email().isBlank()) {
            throw new InvalidUserException("email cannot be null or blank");
        }

        String email = User.normalizeEmail(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }
        if (userRepository.existsByDocument(command.documentType(), command.documentNumber())) {
            throw new DocumentAlreadyExistsException(command.documentType(), command.documentNumber());
        }

        String passwordHash = passwordHasher.hash(command.password());
        Instant now = clock.instant();
        User user = User.create(
                UUID.randomUUID(),
                command.documentType(),
                command.documentNumber(),
                command.fullName(),
                email,
                command.phone(),
                passwordHash,
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                now,
                now);

        return RegisteredCustomer.from(userRepository.save(user));
    }
}
