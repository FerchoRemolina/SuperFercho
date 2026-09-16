package com.superfercho.identity.infrastructure.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.superfercho.identity.application.exception.AddressNotFoundException;
import com.superfercho.identity.application.exception.AddressOwnershipException;
import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.DuplicateDefaultAddressException;
import com.superfercho.identity.application.exception.InactiveAddressException;
import com.superfercho.identity.application.exception.InactiveUserException;
import com.superfercho.identity.application.exception.InvalidCredentialsException;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.domain.exception.InvalidAddressException;
import com.superfercho.identity.domain.exception.InvalidUserException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class IdentityExceptionHandlerTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ADDRESS_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private final IdentityExceptionHandler handler = new IdentityExceptionHandler();

    @Test
    void shouldMapInvalidCredentialsTo401() {
        assertProblem(handler.handleInvalidCredentials(new InvalidCredentialsException()), HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
    }

    @Test
    void shouldMapInactiveUserTo403() {
        assertProblem(handler.handleInactiveUser(new InactiveUserException()), HttpStatus.FORBIDDEN, "USER_INACTIVE");
    }

    @Test
    void shouldMapInvalidRegistrationTo400() {
        assertProblem(
                handler.handleInvalidRegistration(new InvalidRegistrationException("password cannot be null or blank")),
                HttpStatus.BAD_REQUEST,
                "INVALID_REGISTRATION");
    }

    @Test
    void shouldMapInvalidUserTo400() {
        assertProblem(
                handler.handleInvalidUser(new InvalidUserException("email cannot be null or blank")),
                HttpStatus.BAD_REQUEST,
                "INVALID_USER");
    }

    @Test
    void shouldMapInvalidAddressTo400() {
        assertProblem(
                handler.handleInvalidAddress(new InvalidAddressException("city cannot be null or blank")),
                HttpStatus.BAD_REQUEST,
                "INVALID_ADDRESS");
    }

    @Test
    void shouldMapUserAlreadyExistsTo409() {
        assertProblem(
                handler.handleUserAlreadyExists(new UserAlreadyExistsException("ada@identity.test")),
                HttpStatus.CONFLICT,
                "USER_ALREADY_EXISTS");
    }

    @Test
    void shouldMapDocumentAlreadyExistsTo409() {
        assertProblem(
                handler.handleDocumentAlreadyExists(new DocumentAlreadyExistsException("CC", "1234567890")),
                HttpStatus.CONFLICT,
                "DOCUMENT_ALREADY_EXISTS");
    }

    @Test
    void shouldMapDuplicateDefaultAddressTo409() {
        assertProblem(
                handler.handleDuplicateDefaultAddress(new DuplicateDefaultAddressException()),
                HttpStatus.CONFLICT,
                "DUPLICATE_DEFAULT_ADDRESS");
    }

    @Test
    void shouldMapInactiveAddressTo409() {
        assertProblem(
                handler.handleInactiveAddress(new InactiveAddressException(ADDRESS_ID)),
                HttpStatus.CONFLICT,
                "ADDRESS_INACTIVE");
    }

    @Test
    void shouldMapUserNotFoundTo404() {
        assertProblem(handler.handleUserNotFound(new UserNotFoundException(USER_ID)), HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }

    @Test
    void shouldMapAddressNotFoundTo404() {
        assertProblem(
                handler.handleAddressNotFound(new AddressNotFoundException(ADDRESS_ID)),
                HttpStatus.NOT_FOUND,
                "ADDRESS_NOT_FOUND");
    }

    @Test
    void shouldMapAddressOwnershipTo404WithoutDedicatedCode() {
        assertProblem(
                handler.handleAddressNotFound(new AddressOwnershipException(USER_ID, ADDRESS_ID)),
                HttpStatus.NOT_FOUND,
                "ADDRESS_NOT_FOUND");
    }

    private static void assertProblem(ProblemDetail problem, HttpStatus status, String code) {
        assertEquals(status.value(), problem.getStatus());
        assertEquals(status.getReasonPhrase(), problem.getTitle());
        assertEquals(code, problem.getProperties().get("code"));
    }
}
