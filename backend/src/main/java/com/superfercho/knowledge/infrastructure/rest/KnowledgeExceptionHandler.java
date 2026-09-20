package com.superfercho.knowledge.infrastructure.rest;

import com.superfercho.knowledge.application.exception.DocumentNotFoundException;
import com.superfercho.knowledge.application.exception.InvalidSearchRequestException;
import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import com.superfercho.knowledge.domain.exception.InvalidDocumentException;
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
public class KnowledgeExceptionHandler {

    @ExceptionHandler(InvalidDocumentException.class)
    ProblemDetail handleInvalidDocument(InvalidDocumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT", exception.getMessage());
    }

    @ExceptionHandler(InvalidSearchRequestException.class)
    ProblemDetail handleInvalidSearchRequest(InvalidSearchRequestException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_SEARCH_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    ProblemDetail handleDocumentNotFound(DocumentNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(KnowledgeProcessingException.class)
    ProblemDetail handleKnowledgeProcessing(KnowledgeProcessingException exception) {
        return problem(HttpStatus.CONFLICT, "KNOWLEDGE_PROCESSING_FAILED", exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(status.getReasonPhrase());
        problem.setDetail(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
