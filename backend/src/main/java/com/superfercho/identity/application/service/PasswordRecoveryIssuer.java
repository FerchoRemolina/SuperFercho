package com.superfercho.identity.application.service;

import com.superfercho.identity.application.port.EmailMessage;
import com.superfercho.identity.application.port.EmailSender;
import com.superfercho.identity.application.port.PasswordRecoveryTokenRepository;
import com.superfercho.identity.application.port.SecureTokenGenerator;
import com.superfercho.identity.domain.model.PasswordRecoveryToken;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Shared password-recovery issuance: rate-limit per user, consume prior tokens, persist hash, send
 * email. Callers always return a generic public response regardless of whether this method issues.
 */
public final class PasswordRecoveryIssuer {

    private final PasswordRecoveryTokenRepository tokenRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final EmailSender emailSender;
    private final Clock clock;
    private final Duration tokenTtl;
    private final int maxRequestsPerWindow;
    private final Duration requestWindow;
    private final String recoveryBaseUrl;

    public PasswordRecoveryIssuer(
            PasswordRecoveryTokenRepository tokenRepository,
            SecureTokenGenerator tokenGenerator,
            EmailSender emailSender,
            Clock clock,
            Duration tokenTtl,
            int maxRequestsPerWindow,
            Duration requestWindow,
            String recoveryBaseUrl) {
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.emailSender = emailSender;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.requestWindow = requestWindow;
        this.recoveryBaseUrl = trimTrailingSlash(recoveryBaseUrl);
    }

    /**
     * Issues a recovery token and email when the user is an eligible LIVE CUSTOMER. No-ops for
     * deleted, inactive, non-CUSTOMER, or rate-limited accounts.
     */
    public void issueForEligibleCustomer(User user, String clientIp) {
        if (user == null
                || user.role() != Role.CUSTOMER
                || user.deletedAt() != null
                || user.status() != UserStatus.ACTIVE) {
            return;
        }

        Instant now = clock.instant();
        Instant windowStart = now.minus(requestWindow);
        int recentRequests = tokenRepository.findByUserIdCreatedAtOrAfter(user.id(), windowStart).size();
        if (recentRequests >= maxRequestsPerWindow) {
            return;
        }

        String ip = clientIp == null || clientIp.isBlank() ? null : clientIp.trim();
        tokenRepository.consumeActiveTokensForUser(user.id(), now);

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenGenerator.hash(rawToken);
        PasswordRecoveryToken token = PasswordRecoveryToken.create(
                UUID.randomUUID(), user.id(), tokenHash, ip, now, now.plus(tokenTtl));
        tokenRepository.save(token);

        String recoveryUrl = recoveryBaseUrl + "/reset-password?token=" + rawToken;
        emailSender.send(new EmailMessage(
                user.email(),
                "Restablece tu contraseña de SuperFercho",
                buildBody(recoveryUrl)));
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

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("recoveryBaseUrl cannot be blank");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
