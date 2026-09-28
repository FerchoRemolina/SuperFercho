package com.superfercho.identity.infrastructure.rest;

import com.superfercho.identity.application.dto.PasswordRecoveryRequestResult;
import com.superfercho.identity.application.dto.RequestPasswordRecoveryByDocumentCommand;
import com.superfercho.identity.application.dto.RequestPasswordRecoveryCommand;
import com.superfercho.identity.application.dto.ResetPasswordCommand;
import com.superfercho.identity.application.usecase.RequestPasswordRecoveryByDocumentUseCase;
import com.superfercho.identity.application.usecase.RequestPasswordRecoveryUseCase;
import com.superfercho.identity.application.usecase.ResetPasswordUseCase;
import com.superfercho.identity.infrastructure.rest.dto.PasswordRecoveryRequestRestResponse;
import com.superfercho.identity.infrastructure.rest.dto.RequestPasswordRecoveryByDocumentRequest;
import com.superfercho.identity.infrastructure.rest.dto.RequestPasswordRecoveryRequest;
import com.superfercho.identity.infrastructure.rest.dto.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password-recovery")
@Profile("!test")
public class PasswordRecoveryController {

    private final RequestPasswordRecoveryUseCase requestPasswordRecoveryUseCase;
    private final RequestPasswordRecoveryByDocumentUseCase requestPasswordRecoveryByDocumentUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    public PasswordRecoveryController(
            RequestPasswordRecoveryUseCase requestPasswordRecoveryUseCase,
            RequestPasswordRecoveryByDocumentUseCase requestPasswordRecoveryByDocumentUseCase,
            ResetPasswordUseCase resetPasswordUseCase) {
        this.requestPasswordRecoveryUseCase = requestPasswordRecoveryUseCase;
        this.requestPasswordRecoveryByDocumentUseCase = requestPasswordRecoveryByDocumentUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public PasswordRecoveryRequestRestResponse requestRecovery(
            @RequestBody RequestPasswordRecoveryRequest request, HttpServletRequest httpRequest) {
        PasswordRecoveryRequestResult result = requestPasswordRecoveryUseCase.execute(
                new RequestPasswordRecoveryCommand(request.email(), clientIp(httpRequest)));
        return new PasswordRecoveryRequestRestResponse(result.message());
    }

    @PostMapping("/by-document")
    @ResponseStatus(HttpStatus.OK)
    public PasswordRecoveryRequestRestResponse requestRecoveryByDocument(
            @RequestBody RequestPasswordRecoveryByDocumentRequest request, HttpServletRequest httpRequest) {
        PasswordRecoveryRequestResult result = requestPasswordRecoveryByDocumentUseCase.execute(
                new RequestPasswordRecoveryByDocumentCommand(
                        request.documentType(), request.documentNumber(), clientIp(httpRequest)));
        return new PasswordRecoveryRequestRestResponse(result.message());
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(
                new ResetPasswordCommand(request.token(), request.newPassword(), request.confirmPassword()));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
