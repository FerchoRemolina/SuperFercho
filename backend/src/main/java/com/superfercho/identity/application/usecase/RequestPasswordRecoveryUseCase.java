package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.PasswordRecoveryRequestResult;
import com.superfercho.identity.application.dto.RequestPasswordRecoveryCommand;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.exception.PasswordRecoveryRateLimitedException;
import com.superfercho.identity.application.port.PasswordRecoveryAbuseGuard;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.service.PasswordRecoveryIssuer;
import com.superfercho.identity.domain.model.User;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class RequestPasswordRecoveryUseCase {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final PasswordRecoveryIssuer passwordRecoveryIssuer;
    private final PasswordRecoveryAbuseGuard abuseGuard;

    public RequestPasswordRecoveryUseCase(
            UserRepository userRepository,
            PasswordRecoveryIssuer passwordRecoveryIssuer,
            PasswordRecoveryAbuseGuard abuseGuard) {
        this.userRepository = userRepository;
        this.passwordRecoveryIssuer = passwordRecoveryIssuer;
        this.abuseGuard = abuseGuard;
    }

    public PasswordRecoveryRequestResult execute(RequestPasswordRecoveryCommand command) {
        String email = normalizeEmail(command.email());
        requireEmail(email);

        String clientIp = command.clientIp() == null ? "" : command.clientIp().trim();
        if (!abuseGuard.allowRequest(clientIp.isEmpty() ? "unknown" : clientIp)) {
            throw new PasswordRecoveryRateLimitedException();
        }

        Optional<User> user = userRepository.findByEmail(email);
        user.ifPresent(value -> passwordRecoveryIssuer.issueForEligibleCustomer(value, clientIp));

        return PasswordRecoveryRequestResult.generic();
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void requireEmail(String email) {
        if (email.isBlank() || !EMAIL.matcher(email).matches()) {
            throw new InvalidRegistrationException("Ingresa un correo electrónico válido.");
        }
    }
}
