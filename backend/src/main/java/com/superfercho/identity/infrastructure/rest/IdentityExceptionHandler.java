package com.superfercho.identity.infrastructure.rest;

import com.superfercho.identity.application.exception.AddressNotFoundException;
import com.superfercho.identity.application.exception.AddressOwnershipException;
import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.DuplicateDefaultAddressException;
import com.superfercho.identity.application.exception.InactiveAddressException;
import com.superfercho.identity.application.exception.InactiveUserException;
import com.superfercho.identity.application.exception.InvalidCredentialsException;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.exception.StorefrontPreviewExpiredException;
import com.superfercho.identity.application.exception.StorefrontPreviewForbiddenException;
import com.superfercho.identity.application.exception.StorefrontPreviewNotFoundException;
import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.domain.exception.InvalidAddressException;
import com.superfercho.identity.domain.exception.InvalidUserException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityExceptionHandler {

    @ExceptionHandler(UnauthenticatedUserException.class)
    ProblemDetail handleUnauthenticated(UnauthenticatedUserException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage());
    }

    @ExceptionHandler(InactiveUserException.class)
    ProblemDetail handleInactiveUser(InactiveUserException exception) {
        return problem(HttpStatus.FORBIDDEN, "USER_INACTIVE", exception.getMessage());
    }

    @ExceptionHandler(InvalidRegistrationException.class)
    ProblemDetail handleInvalidRegistration(InvalidRegistrationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REGISTRATION", exception.getMessage());
    }

    @ExceptionHandler(InvalidUserException.class)
    ProblemDetail handleInvalidUser(InvalidUserException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_USER", exception.getMessage());
    }

    @ExceptionHandler(InvalidAddressException.class)
    ProblemDetail handleInvalidAddress(InvalidAddressException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_ADDRESS", exception.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException exception) {
        return problem(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(DocumentAlreadyExistsException.class)
    ProblemDetail handleDocumentAlreadyExists(DocumentAlreadyExistsException exception) {
        return problem(HttpStatus.CONFLICT, "DOCUMENT_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(DuplicateDefaultAddressException.class)
    ProblemDetail handleDuplicateDefaultAddress(DuplicateDefaultAddressException exception) {
        return problem(HttpStatus.CONFLICT, "DUPLICATE_DEFAULT_ADDRESS", exception.getMessage());
    }

    @ExceptionHandler(InactiveAddressException.class)
    ProblemDetail handleInactiveAddress(InactiveAddressException exception) {
        return problem(HttpStatus.CONFLICT, "ADDRESS_INACTIVE", exception.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({AddressNotFoundException.class, AddressOwnershipException.class})
    ProblemDetail handleAddressNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(StorefrontPreviewNotFoundException.class)
    ProblemDetail handleStorefrontPreviewNotFound(StorefrontPreviewNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "STOREFRONT_PREVIEW_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(StorefrontPreviewForbiddenException.class)
    ProblemDetail handleStorefrontPreviewForbidden(StorefrontPreviewForbiddenException exception) {
        return problem(HttpStatus.FORBIDDEN, "STOREFRONT_PREVIEW_FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(StorefrontPreviewExpiredException.class)
    ProblemDetail handleStorefrontPreviewExpired(StorefrontPreviewExpiredException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "STOREFRONT_PREVIEW_EXPIRED", exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(status.getReasonPhrase());
        problem.setDetail(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
