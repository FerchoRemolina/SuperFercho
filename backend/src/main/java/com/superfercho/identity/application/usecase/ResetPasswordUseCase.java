package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.ResetPasswordCommand;
import com.superfercho.identity.application.exception.InvalidPasswordRecoveryException;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.PasswordRecoveryTokenRepository;
import com.superfercho.identity.application.port.SecureTokenGenerator;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.validation.CustomerRegistrationRules;
import com.superfercho.identity.domain.model.PasswordRecoveryToken;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class ResetPasswordUseCase {

    private final PasswordRecoveryTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public ResetPasswordUseCase(
            PasswordRecoveryTokenRepository tokenRepository,
            UserRepository userRepository,
            SecureTokenGenerator tokenGenerator,
            PasswordHasher passwordHasher,
            Clock clock) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.tokenGenerator = tokenGenerator;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    public void execute(ResetPasswordCommand command) {
        if (command.token() == null || command.token().isBlank()) {
            throw InvalidPasswordRecoveryException.invalidToken();
        }
        if (!Objects.equals(command.newPassword(), command.confirmPassword())) {
            throw InvalidPasswordRecoveryException.passwordMismatch();
        }
        try {
            CustomerRegistrationRules.requirePassword(command.newPassword());
        } catch (InvalidRegistrationException ex) {
            throw new InvalidPasswordRecoveryException(ex.getMessage());
        }

        Instant now = clock.instant();
        String tokenHash = tokenGenerator.hash(command.token().trim());
        PasswordRecoveryToken token = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(InvalidPasswordRecoveryException::invalidToken);

        if (!token.isUsable(now)) {
            throw InvalidPasswordRecoveryException.invalidToken();
        }

        User user = userRepository
                .findById(token.userId())
                .orElseThrow(InvalidPasswordRecoveryException::invalidToken);
        if (user.status() != UserStatus.ACTIVE) {
            throw InvalidPasswordRecoveryException.invalidToken();
        }

        String passwordHash = passwordHasher.hash(command.newPassword());
        User updated = User.create(
                user.id(),
                user.documentType(),
                user.documentNumber(),
                user.firstName(),
                user.lastName(),
                user.email(),
                user.phone(),
                passwordHash,
                user.role(),
                user.status(),
                user.customerRecordId(),
                user.deletedAt(),
                user.createdAt(),
                now);
        userRepository.save(updated);
        tokenRepository.save(token.consume(now));
    }
}
