package com.superfercho.identity.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.identity.application.dto.PasswordRecoveryRequestResult;
import com.superfercho.identity.application.dto.RequestPasswordRecoveryByDocumentCommand;
import com.superfercho.identity.application.dto.RequestPasswordRecoveryCommand;
import com.superfercho.identity.application.dto.ResetPasswordCommand;
import com.superfercho.identity.application.exception.InvalidPasswordRecoveryException;
import com.superfercho.identity.application.usecase.RequestPasswordRecoveryByDocumentUseCase;
import com.superfercho.identity.application.usecase.RequestPasswordRecoveryUseCase;
import com.superfercho.identity.application.usecase.ResetPasswordUseCase;
import com.superfercho.platform.error.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PasswordRecoveryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({IdentityExceptionHandler.class, ApiExceptionHandler.class})
class PasswordRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestPasswordRecoveryUseCase requestPasswordRecoveryUseCase;

    @MockitoBean
    private RequestPasswordRecoveryByDocumentUseCase requestPasswordRecoveryByDocumentUseCase;

    @MockitoBean
    private ResetPasswordUseCase resetPasswordUseCase;

    @Test
    void shouldRequestRecovery() throws Exception {
        when(requestPasswordRecoveryUseCase.execute(any()))
                .thenReturn(PasswordRecoveryRequestResult.generic());

        mockMvc.perform(post("/api/v1/auth/password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ada@identity.test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordRecoveryRequestResult.GENERIC_MESSAGE));

        verify(requestPasswordRecoveryUseCase)
                .execute(new RequestPasswordRecoveryCommand("ada@identity.test", "127.0.0.1"));
    }

    @Test
    void shouldRequestRecoveryByDocument() throws Exception {
        when(requestPasswordRecoveryByDocumentUseCase.execute(any()))
                .thenReturn(PasswordRecoveryRequestResult.genericForDocument());

        mockMvc.perform(post("/api/v1/auth/password-recovery/by-document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentType": "CC", "documentNumber": "12345678"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value(PasswordRecoveryRequestResult.GENERIC_MESSAGE_BY_DOCUMENT))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.documentNumber").doesNotExist());

        verify(requestPasswordRecoveryByDocumentUseCase)
                .execute(new RequestPasswordRecoveryByDocumentCommand("CC", "12345678", "127.0.0.1"));
    }

    @Test
    void shouldResetPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-recovery/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "raw-token",
                                  "newPassword": "Luis123!",
                                  "confirmPassword": "Luis123!"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(resetPasswordUseCase)
                .execute(new ResetPasswordCommand("raw-token", "Luis123!", "Luis123!"));
    }

    @Test
    void shouldMapInvalidToken() throws Exception {
        org.mockito.Mockito.doThrow(InvalidPasswordRecoveryException.invalidToken())
                .when(resetPasswordUseCase)
                .execute(any());

        mockMvc.perform(post("/api/v1/auth/password-recovery/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "bad",
                                  "newPassword": "Luis123!",
                                  "confirmPassword": "Luis123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RECOVERY"));
    }
}
