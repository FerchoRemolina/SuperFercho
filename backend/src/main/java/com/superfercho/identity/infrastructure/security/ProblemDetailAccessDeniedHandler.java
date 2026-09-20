package com.superfercho.identity.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

final class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityProblemDetailResponses responses;

    ProblemDetailAccessDeniedHandler(SecurityProblemDetailResponses responses) {
        this.responses = responses;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        responses.writeAccessDenied(response);
    }
}
