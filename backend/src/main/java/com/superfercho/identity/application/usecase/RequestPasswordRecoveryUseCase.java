package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.PasswordRecoveryRequestResult;
import com.superfercho.identity.application.dto.RequestPasswordRecoveryCommand;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.exception.PasswordRecoveryRateLimitedException;
import com.superfercho.identity.application.port.EmailMessage;
import com.superfercho.identity.application.port.EmailSender;
import com.superfercho.identity.application.port.PasswordRecoveryAbuseGuard;
import com.superfercho.identity.application.port.PasswordRecoveryTokenRepository;
import com.superfercho.identity.application.port.SecureTokenGenerator;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.PasswordRecoveryToken;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class RequestPasswordRecoveryUseCase {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final PasswordRecoveryTokenRepository tokenRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final EmailSender emailSender;
    private final PasswordRecoveryAbuseGuard abuseGuard;
    private final Clock clock;
    private final Duration tokenTtl;
    private final int maxRequestsPerWindow;
    private final Duration requestWindow;
    private final String recoveryBaseUrl;

    public RequestPasswordRecoveryUseCase(
            UserRepository userRepository,
            PasswordRecoveryTokenRepository tokenRepository,
            SecureTokenGenerator tokenGenerator,
            EmailSender emailSender,
            PasswordRecoveryAbuseGuard abuseGuard,
            Clock clock,
            Duration tokenTtl,
            int maxRequestsPerWindow,
            Duration requestWindow,
            String recoveryBaseUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.emailSender = emailSender;
        this.abuseGuard = abuseGuard;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.requestWindow = requestWindow;
        this.recoveryBaseUrl = trimTrailingSlash(recoveryBaseUrl);
    }

    public PasswordRecoveryRequestResult execute(RequestPasswordRecoveryCommand command) {
        String email = normalizeEmail(command.email());
        requireEmail(email);

        String clientIp = command.clientIp() == null ? "" : command.clientIp().trim();
        if (!abuseGuard.allowRequest(clientIp.isEmpty() ? "unknown" : clientIp)) {
            throw new PasswordRecoveryRateLimitedException();
        }

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()
                || user.get().deletedAt() != null
                || user.get().status() != UserStatus.ACTIVE) {
            return PasswordRecoveryRequestResult.generic();
        }

        Instant now = clock.instant();
        Instant windowStart = now.minus(requestWindow);
        int recentRequests =
                tokenRepository.findByUserIdCreatedAtOrAfter(user.get().id(), windowStart).size();
        if (recentRequests >= maxRequestsPerWindow) {
            return PasswordRecoveryRequestResult.generic();
        }

        tokenRepository.consumeActiveTokensForUser(user.get().id(), now);

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenGenerator.hash(rawToken);
        PasswordRecoveryToken token = PasswordRecoveryToken.create(
                UUID.randomUUID(),
                user.get().id(),
                tokenHash,
                clientIp.isEmpty() ? null : clientIp,
                now,
                now.plus(tokenTtl));
        tokenRepository.save(token);

        String recoveryUrl = recoveryBaseUrl + "/reset-password?token=" + rawToken;
        emailSender.send(new EmailMessage(
                email,
                "Restablece tu contraseña de SuperFercho",
                buildBody(recoveryUrl)));

        return PasswordRecoveryRequestResult.generic();
    }

    private static String buildBody(String recoveryUrl) {
        return """
                Hola,

                Recibimos una solicitud para restablecer tu contraseña en SuperFercho.

                Usa este enlace (válido por 15 minutos):
                %s

                Si no solicitaste este cambio, puedes ignorar este mensaje.

                Equipo SuperFercho
                """
                .formatted(recoveryUrl);
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

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("recoveryBaseUrl cannot be blank");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
