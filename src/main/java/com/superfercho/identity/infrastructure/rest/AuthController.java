package com.superfercho.identity.infrastructure.rest;

import com.superfercho.identity.application.dto.AuthenticateUserCommand;
import com.superfercho.identity.application.usecase.AuthenticateUserUseCase;
import com.superfercho.identity.infrastructure.rest.dto.AuthenticateUserRequest;
import com.superfercho.identity.infrastructure.rest.dto.AuthenticationRestResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
    }

    @PostMapping("/login")
    public AuthenticationRestResponse login(@RequestBody AuthenticateUserRequest request) {
        return AuthenticationRestResponse.from(
                authenticateUserUseCase.execute(new AuthenticateUserCommand(request.email(), request.password())));
    }
}
