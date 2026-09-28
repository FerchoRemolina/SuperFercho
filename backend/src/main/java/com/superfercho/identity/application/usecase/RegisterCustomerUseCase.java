package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.validation.CustomerRegistrationRules;
import com.superfercho.identity.application.validation.CustomerRegistrationRules.ValidatedRegistration;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Registers a CUSTOMER account. Creates or reuses {@link CustomerRecord} by document; never
 * overwrites commercial record data on re-registration. Infrastructure wraps {@link #execute} in a
 * local transaction.
 */
public class RegisterCustomerUseCase {

    private final UserRepository userRepository;
    private final CustomerRecordRepository customerRecordRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public RegisterCustomerUseCase(
            UserRepository userRepository,
            CustomerRecordRepository customerRecordRepository,
            PasswordHasher passwordHasher,
            Clock clock) {
        this.userRepository = userRepository;
        this.customerRecordRepository = customerRecordRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    public RegisteredCustomer execute(RegisterCustomerCommand command) {
        ValidatedRegistration validated = CustomerRegistrationRules.validate(command);

        if (userRepository.existsByEmail(validated.email())) {
            throw new UserAlreadyExistsException(validated.email());
        }

        Instant now = clock.instant();
        Optional<CustomerRecord> existingRecord = customerRecordRepository.findByDocument(
                validated.documentType(), validated.documentNumber());

        UUID customerRecordId;
        if (existingRecord.isPresent()) {
            CustomerRecord record = existingRecord.get();
            if (userRepository.findLiveByCustomerRecordId(record.id()).isPresent()) {
                throw new DocumentAlreadyExistsException(
                        validated.documentType(), validated.documentNumber());
            }
            customerRecordId = record.id();
        } else {
            CustomerRecord created = customerRecordRepository.save(CustomerRecord.create(
                    UUID.randomUUID(),
                    validated.documentType(),
                    validated.documentNumber(),
                    validated.firstName(),
                    validated.lastName(),
                    now,
                    now));
            customerRecordId = created.id();
        }

        String passwordHash = passwordHasher.hash(validated.password());
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
                customerRecordId,
                null,
                now,
                now);

        return RegisteredCustomer.from(userRepository.save(user));
    }
}
