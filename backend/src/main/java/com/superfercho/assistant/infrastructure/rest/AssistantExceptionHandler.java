package com.superfercho.assistant.infrastructure.rest;

import com.superfercho.assistant.application.exception.ConversationNotFoundException;
import com.superfercho.assistant.application.exception.InvalidChatRequestException;
import com.superfercho.assistant.application.exception.InvalidConfirmationException;
import com.superfercho.assistant.application.exception.InvalidPackageSelectionException;
import com.superfercho.assistant.application.exception.InvalidToolArgumentsException;
import com.superfercho.assistant.application.exception.LlmProviderException;
import com.superfercho.assistant.application.exception.ToolNotAllowedException;
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
public class AssistantExceptionHandler {

    @ExceptionHandler(InvalidChatRequestException.class)
    ProblemDetail handleInvalidChatRequest(InvalidChatRequestException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_CHAT_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(InvalidConfirmationException.class)
    ProblemDetail handleInvalidConfirmation(InvalidConfirmationException ignored) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_CONFIRMATION", "Invalid confirmation");
    }

    @ExceptionHandler(InvalidPackageSelectionException.class)
    ProblemDetail handleInvalidPackageSelection(InvalidPackageSelectionException ignored) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PACKAGE_SELECTION", "Invalid package selection");
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    ProblemDetail handleConversationNotFound(ConversationNotFoundException ignored) {
        return problem(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Conversation not found");
    }

    @ExceptionHandler(com.superfercho.assistant.application.exception.FerchoPausedException.class)
    ProblemDetail handleFerchoPaused(com.superfercho.assistant.application.exception.FerchoPausedException exception) {
        var problem = problem(HttpStatus.TOO_MANY_REQUESTS, exception.code(), exception.getMessage());
        problem.setProperty("remainingSeconds", exception.remainingSeconds());
        problem.setProperty("pausedUntil", exception.pausedUntil().toString());
        return problem;
    }

    @ExceptionHandler(com.superfercho.assistant.application.fercho.FerchoUsageLedgerUnavailableException.class)
    ProblemDetail handleFerchoLedgerUnavailable(com.superfercho.assistant.application.fercho.FerchoUsageLedgerUnavailableException ignored) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "FERCHO_UNAVAILABLE", "Fercho no est\u00e1 disponible en este momento");
    }

    @ExceptionHandler(LlmProviderException.class)
    ProblemDetail handleLlmProvider(LlmProviderException ignored) {
        return problem(HttpStatus.CONFLICT, "LLM_PROVIDER_FAILED", "LLM provider request failed");
    }

    @ExceptionHandler(InvalidToolArgumentsException.class)
    ProblemDetail handleInvalidToolArguments(InvalidToolArgumentsException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_TOOL_ARGUMENTS", exception.getMessage());
    }

    @ExceptionHandler(ToolNotAllowedException.class)
    ProblemDetail handleToolNotAllowed(ToolNotAllowedException exception) {
        return problem(HttpStatus.BAD_REQUEST, "TOOL_NOT_ALLOWED", exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(status.getReasonPhrase());
        problem.setDetail(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
