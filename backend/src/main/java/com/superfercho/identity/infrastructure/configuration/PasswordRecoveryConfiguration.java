package com.superfercho.identity.infrastructure.configuration;

import com.superfercho.identity.application.port.EmailSender;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.PasswordRecoveryAbuseGuard;
import com.superfercho.identity.application.port.PasswordRecoveryTokenRepository;
import com.superfercho.identity.application.port.SecureTokenGenerator;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.usecase.RequestPasswordRecoveryUseCase;
import com.superfercho.identity.application.usecase.ResetPasswordUseCase;
import com.superfercho.identity.infrastructure.mail.ConsoleEmailSender;
import com.superfercho.identity.infrastructure.mail.EmailProperties;
import com.superfercho.identity.infrastructure.mail.InMemoryPasswordRecoveryAbuseGuard;
import com.superfercho.identity.infrastructure.mail.NoOpEmailSender;
import com.superfercho.identity.infrastructure.security.Sha256SecureTokenGenerator;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@ConditionalOnBean(UserRepository.class)
@EnableConfigurationProperties({EmailProperties.class, PasswordRecoveryProperties.class})
public class PasswordRecoveryConfiguration {

    @Bean
    SecureTokenGenerator secureTokenGenerator() {
        return new Sha256SecureTokenGenerator();
    }

    @Bean
    EmailSender emailSender(EmailProperties properties) {
        String provider = properties.provider() == null ? "console" : properties.provider().trim().toLowerCase();
        return switch (provider) {
            case "console" -> new ConsoleEmailSender();
            case "noop" -> new NoOpEmailSender();
            // Real providers (resend, brevo, smtp) plug in here later.
            default -> throw new IllegalStateException(
                    "Unsupported email provider '"
                            + provider
                            + "'. Use 'console' (local) or 'noop' until a real adapter is configured.");
        };
    }

    @Bean
    PasswordRecoveryAbuseGuard passwordRecoveryAbuseGuard(
            Clock clock, PasswordRecoveryProperties properties) {
        return new InMemoryPasswordRecoveryAbuseGuard(
                clock, properties.ipMaxRequestsPerWindow(), properties.ipWindow());
    }

    @Bean
    @ConditionalOnBean(PasswordRecoveryTokenRepository.class)
    RequestPasswordRecoveryUseCase requestPasswordRecoveryUseCase(
            UserRepository userRepository,
            PasswordRecoveryTokenRepository tokenRepository,
            SecureTokenGenerator tokenGenerator,
            EmailSender emailSender,
            PasswordRecoveryAbuseGuard abuseGuard,
            Clock clock,
            PasswordRecoveryProperties recoveryProperties,
            EmailProperties emailProperties) {
        return new RequestPasswordRecoveryUseCase(
                userRepository,
                tokenRepository,
                tokenGenerator,
                emailSender,
                abuseGuard,
                clock,
                recoveryProperties.tokenTtl(),
                recoveryProperties.maxRequestsPerWindow(),
                recoveryProperties.requestWindow(),
                emailProperties.recoveryBaseUrl());
    }

    @Bean
    @ConditionalOnBean(PasswordRecoveryTokenRepository.class)
    ResetPasswordUseCase resetPasswordUseCase(
            PasswordRecoveryTokenRepository tokenRepository,
            UserRepository userRepository,
            SecureTokenGenerator tokenGenerator,
            PasswordHasher passwordHasher,
            Clock clock) {
        return new ResetPasswordUseCase(
                tokenRepository, userRepository, tokenGenerator, passwordHasher, clock);
    }
}
