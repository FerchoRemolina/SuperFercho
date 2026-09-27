package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.validation.CustomerRegistrationRules;
import com.superfercho.identity.application.validation.CustomerRegistrationRules.ValidatedRegistration;
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
        ValidatedRegistration validated = CustomerRegistrationRules.validate(command);

        if (userRepository.existsByEmail(validated.email())) {
            throw new UserAlreadyExistsException(validated.email());
        }
        if (userRepository.existsByDocument(validated.documentType(), validated.documentNumber())) {
            throw new DocumentAlreadyExistsException(
                    validated.documentType(), validated.documentNumber());
        }

        String passwordHash = passwordHasher.hash(validated.password());
        Instant now = clock.instant();
        User user = User.create(
                UUID.randomUUID(),
                validated.documentType(),
                validated.documentNumber(),
                validated.firstName(),
                validated.lastName(),
                validated.email(),
                validated.phone(),
                passwordHash,
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                now,
                now);

        return RegisteredCustomer.from(userRepository.save(user));
    }
}
