package com.superfercho.identity.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

final class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityProblemDetailResponses responses;

    ProblemDetailAuthenticationEntryPoint(SecurityProblemDetailResponses responses) {
        this.responses = responses;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        responses.writeUnauthenticated(response);
    }
}
