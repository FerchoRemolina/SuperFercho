package com.superfercho.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

final class SecurityProblemDetailResponses {

    static final String UNAUTHENTICATED_CODE = "UNAUTHENTICATED";
    static final String ACCESS_DENIED_CODE = "ACCESS_DENIED";
    static final String UNAUTHENTICATED_DETAIL = "Authenticated user is required";
    static final String ACCESS_DENIED_DETAIL = "Access is denied";

    private final ObjectMapper objectMapper;

    SecurityProblemDetailResponses(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void writeUnauthenticated(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, UNAUTHENTICATED_CODE, UNAUTHENTICATED_DETAIL);
    }

    void writeAccessDenied(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ACCESS_DENIED_CODE, ACCESS_DENIED_DETAIL);
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String detail)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(status.getReasonPhrase());
        problem.setDetail(detail);
        problem.setProperty("code", code);
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
