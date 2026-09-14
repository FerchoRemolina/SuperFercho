package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AuthenticateUserCommand;
import com.superfercho.identity.application.dto.AuthenticationResult;
import com.superfercho.identity.application.exception.InactiveUserException;
import com.superfercho.identity.application.exception.InvalidCredentialsException;
import com.superfercho.identity.application.port.AccessTokenIssuer;
import com.superfercho.identity.application.port.IssuedAccessToken;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;

public final class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;

    public AuthenticateUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    public AuthenticationResult execute(AuthenticateUserCommand command) {
        if (command.email() == null || command.email().isBlank()
                || command.password() == null
                || command.password().isBlank()) {
            throw new InvalidCredentialsException();
        }

        String email = User.normalizeEmail(command.email());
        User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (user.status() != UserStatus.ACTIVE) {
            throw new InactiveUserException(user.id());
        }
        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        IssuedAccessToken token = accessTokenIssuer.issue(user.id(), user.role());
        return new AuthenticationResult(user.id(), user.role(), token.token(), token.expiresAt());
    }
}
