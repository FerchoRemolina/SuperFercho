package com.superfercho.identity.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.identity.application.dto.AddAddressCommand;
import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.dto.DeactivateAddressCommand;
import com.superfercho.identity.application.dto.ListAddressesCommand;
import com.superfercho.identity.application.dto.SetDefaultAddressCommand;
import com.superfercho.identity.application.dto.UpdateAddressCommand;
import com.superfercho.identity.application.exception.AddressNotFoundException;
import com.superfercho.identity.application.exception.AddressOwnershipException;
import com.superfercho.identity.application.exception.InactiveAddressException;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.application.usecase.AddAddressUseCase;
import com.superfercho.identity.application.usecase.DeactivateAddressUseCase;
import com.superfercho.identity.application.usecase.ListAddressesUseCase;
import com.superfercho.identity.application.usecase.SetDefaultAddressUseCase;
import com.superfercho.identity.application.usecase.UpdateAddressUseCase;
import com.superfercho.identity.domain.exception.InvalidAddressException;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.platform.error.ApiExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AddressController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({IdentityExceptionHandler.class, ApiExceptionHandler.class})
class AddressControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-03-01T10:30:00Z");
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ADDRESS_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddAddressUseCase addAddressUseCase;

    @MockitoBean
    private ListAddressesUseCase listAddressesUseCase;

    @MockitoBean
    private UpdateAddressUseCase updateAddressUseCase;

    @MockitoBean
    private DeactivateAddressUseCase deactivateAddressUseCase;

    @MockitoBean
    private SetDefaultAddressUseCase setDefaultAddressUseCase;

    @Test
    void shouldAddAddressForUserInPath() throws Exception {
        when(addAddressUseCase.execute(any())).thenReturn(addressResult(true, AddressStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/customers/{userId}/addresses", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addAddressJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.endsWith(
                                "/api/v1/customers/" + USER_ID + "/addresses/" + ADDRESS_ID)))
                .andExpect(jsonPath("$.id").value(ADDRESS_ID.toString()))
                .andExpect(jsonPath("$.label").value("Casa"))
                .andExpect(jsonPath("$.recipientName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.addressLine").value("Calle 1 # 2-3"))
                .andExpect(jsonPath("$.additionalInfo").value("Apto 101"))
                .andExpect(jsonPath("$.city").value("Bogotá"))
                .andExpect(jsonPath("$.department").value("Cundinamarca"))
                .andExpect(jsonPath("$.phone").value("3001234567"))
                .andExpect(jsonPath("$.isDefault").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(addAddressUseCase)
                .execute(new AddAddressCommand(
                        USER_ID,
                        "Casa",
                        "Ada Lovelace",
                        "Calle 1 # 2-3",
                        "Apto 101",
                        "Bogotá",
                        "Cundinamarca",
                        "3001234567",
                        true));
        verifyNoInteractions(listAddressesUseCase, updateAddressUseCase, deactivateAddressUseCase, setDefaultAddressUseCase);
    }

    @Test
    void shouldListAddressesForUserInPath() throws Exception {
        when(listAddressesUseCase.execute(new ListAddressesCommand(USER_ID)))
                .thenReturn(List.of(addressResult(true, AddressStatus.ACTIVE)));

        mockMvc.perform(get("/api/v1/customers/{userId}/addresses", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ADDRESS_ID.toString()))
                .andExpect(jsonPath("$[0].city").value("Bogotá"))
                .andExpect(jsonPath("$[0].isDefault").value(true));

        verify(listAddressesUseCase).execute(new ListAddressesCommand(USER_ID));
        verify(listAddressesUseCase, never()).execute(new ListAddressesCommand(OTHER_USER_ID));
        verifyNoInteractions(addAddressUseCase, updateAddressUseCase, deactivateAddressUseCase, setDefaultAddressUseCase);
    }

    @Test
    void shouldUpdateAddress() throws Exception {
        when(updateAddressUseCase.execute(any())).thenReturn(addressResult(false, AddressStatus.ACTIVE));

        mockMvc.perform(put("/api/v1/customers/{userId}/addresses/{addressId}", USER_ID, ADDRESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateAddressJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ADDRESS_ID.toString()))
                .andExpect(jsonPath("$.label").value("Casa"));

        verify(updateAddressUseCase)
                .execute(new UpdateAddressCommand(
                        USER_ID,
                        ADDRESS_ID,
                        "Oficina",
                        "Ada Lovelace",
                        "Calle 9 # 8-7",
                        "Piso 2",
                        "Medellín",
                        "Antioquia",
                        "3009876543"));
        verifyNoInteractions(addAddressUseCase, listAddressesUseCase, deactivateAddressUseCase, setDefaultAddressUseCase);
    }

    @Test
    void shouldDeactivateAddress() throws Exception {
        when(deactivateAddressUseCase.execute(new DeactivateAddressCommand(USER_ID, ADDRESS_ID)))
                .thenReturn(addressResult(false, AddressStatus.INACTIVE));

        mockMvc.perform(delete("/api/v1/customers/{userId}/addresses/{addressId}", USER_ID, ADDRESS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ADDRESS_ID.toString()))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(deactivateAddressUseCase).execute(new DeactivateAddressCommand(USER_ID, ADDRESS_ID));
        verifyNoInteractions(addAddressUseCase, listAddressesUseCase, updateAddressUseCase, setDefaultAddressUseCase);
    }

    @Test
    void shouldSetDefaultAddress() throws Exception {
        when(setDefaultAddressUseCase.execute(new SetDefaultAddressCommand(USER_ID, ADDRESS_ID)))
                .thenReturn(addressResult(true, AddressStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/customers/{userId}/addresses/{addressId}/default", USER_ID, ADDRESS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ADDRESS_ID.toString()))
                .andExpect(jsonPath("$.isDefault").value(true));

        verify(setDefaultAddressUseCase).execute(new SetDefaultAddressCommand(USER_ID, ADDRESS_ID));
        verifyNoInteractions(addAddressUseCase, listAddressesUseCase, updateAddressUseCase, deactivateAddressUseCase);
    }

    @Test
    void shouldMapUserNotFoundToNotFound() throws Exception {
        when(listAddressesUseCase.execute(any())).thenThrow(new UserNotFoundException(USER_ID));

        mockMvc.perform(get("/api/v1/customers/{userId}/addresses", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void shouldMapAddressNotFoundToNotFound() throws Exception {
        when(updateAddressUseCase.execute(any())).thenThrow(new AddressNotFoundException(ADDRESS_ID));

        mockMvc.perform(put("/api/v1/customers/{userId}/addresses/{addressId}", USER_ID, ADDRESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateAddressJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADDRESS_NOT_FOUND"));
    }

    @Test
    void shouldMapAddressOwnershipToNotFoundWithoutDedicatedCode() throws Exception {
        when(updateAddressUseCase.execute(any()))
                .thenThrow(new AddressOwnershipException(OTHER_USER_ID, ADDRESS_ID));

        mockMvc.perform(put("/api/v1/customers/{userId}/addresses/{addressId}", OTHER_USER_ID, ADDRESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateAddressJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADDRESS_NOT_FOUND"));
    }

    @Test
    void shouldMapInactiveAddressToConflict() throws Exception {
        when(updateAddressUseCase.execute(any())).thenThrow(new InactiveAddressException(ADDRESS_ID));

        mockMvc.perform(put("/api/v1/customers/{userId}/addresses/{addressId}", USER_ID, ADDRESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateAddressJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADDRESS_INACTIVE"));
    }

    @Test
    void shouldMapInvalidAddressToBadRequest() throws Exception {
        when(addAddressUseCase.execute(any())).thenThrow(new InvalidAddressException("city cannot be null or blank"));

        mockMvc.perform(post("/api/v1/customers/{userId}/addresses", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addAddressJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ADDRESS"));
    }

    private static String addAddressJson() {
        return """
                {
                  "label": "Casa",
                  "recipientName": "Ada Lovelace",
                  "addressLine": "Calle 1 # 2-3",
                  "additionalInfo": "Apto 101",
                  "city": "Bogotá",
                  "department": "Cundinamarca",
                  "phone": "3001234567",
                  "isDefault": true
                }
                """;
    }

    private static String updateAddressJson() {
        return """
                {
                  "label": "Oficina",
                  "recipientName": "Ada Lovelace",
                  "addressLine": "Calle 9 # 8-7",
                  "additionalInfo": "Piso 2",
                  "city": "Medellín",
                  "department": "Antioquia",
                  "phone": "3009876543"
                }
                """;
    }

    private static AddressResult addressResult(boolean isDefault, AddressStatus status) {
        return new AddressResult(
                ADDRESS_ID,
                "Casa",
                "Ada Lovelace",
                "Calle 1 # 2-3",
                "Apto 101",
                "Bogotá",
                "Cundinamarca",
                "3001234567",
                isDefault,
                status,
                CREATED_AT,
                UPDATED_AT);
    }
}
