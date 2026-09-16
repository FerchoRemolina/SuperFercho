package com.superfercho.identity.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import com.superfercho.identity.application.usecase.RegisterCustomerUseCase;
import com.superfercho.identity.domain.exception.InvalidUserException;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.platform.error.ApiExceptionHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({IdentityExceptionHandler.class, ApiExceptionHandler.class})
class CustomerControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterCustomerUseCase registerCustomerUseCase;

    @Test
    void shouldRegisterCustomer() throws Exception {
        when(registerCustomerUseCase.execute(any())).thenReturn(registeredCustomer());

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson()))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.documentType").value("CC"))
                .andExpect(jsonPath("$.documentNumber").value("1234567890"))
                .andExpect(jsonPath("$.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@identity.test"))
                .andExpect(jsonPath("$.phone").value("3001234567"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(registerCustomerUseCase)
                .execute(new RegisterCustomerCommand(
                        "CC",
                        "1234567890",
                        "Ada Lovelace",
                        "ada@identity.test",
                        "3001234567",
                        "secret-password"));
    }

    @Test
    void shouldMapUserAlreadyExistsToConflict() throws Exception {
        when(registerCustomerUseCase.execute(any())).thenThrow(new UserAlreadyExistsException("ada@identity.test"));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
    }

    @Test
    void shouldMapDocumentAlreadyExistsToConflict() throws Exception {
        when(registerCustomerUseCase.execute(any()))
                .thenThrow(new DocumentAlreadyExistsException("CC", "1234567890"));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENT_ALREADY_EXISTS"));
    }

    @Test
    void shouldMapInvalidRegistrationToBadRequest() throws Exception {
        when(registerCustomerUseCase.execute(any()))
                .thenThrow(new InvalidRegistrationException("password cannot be null or blank"));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REGISTRATION"));
    }

    @Test
    void shouldMapInvalidUserToBadRequest() throws Exception {
        when(registerCustomerUseCase.execute(any())).thenThrow(new InvalidUserException("email cannot be null or blank"));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_USER"));
    }

    private static String registerJson() {
        return """
                {
                  "documentType": "CC",
                  "documentNumber": "1234567890",
                  "fullName": "Ada Lovelace",
                  "email": "ada@identity.test",
                  "phone": "3001234567",
                  "password": "secret-password"
                }
                """;
    }

    private static RegisteredCustomer registeredCustomer() {
        return new RegisteredCustomer(
                CUSTOMER_ID,
                "CC",
                "1234567890",
                "Ada Lovelace",
                "ada@identity.test",
                "3001234567",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                CREATED_AT);
    }
}
