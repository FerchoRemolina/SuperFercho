package com.superfercho.knowledge.infrastructure.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.superfercho.knowledge.application.exception.DocumentNotFoundException;
import com.superfercho.knowledge.application.exception.InvalidSearchRequestException;
import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import com.superfercho.knowledge.domain.exception.InvalidDocumentException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class KnowledgeExceptionHandlerTest {

    private static final UUID DOCUMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final KnowledgeExceptionHandler handler = new KnowledgeExceptionHandler();

    @Test
    void shouldMapInvalidDocumentTo400() {
        assertProblem(
                handler.handleInvalidDocument(new InvalidDocumentException("title cannot be null or blank")),
                HttpStatus.BAD_REQUEST,
                "INVALID_DOCUMENT");
    }

    @Test
    void shouldMapInvalidSearchRequestTo400() {
        assertProblem(
                handler.handleInvalidSearchRequest(new InvalidSearchRequestException("limit must be between 1 and 20")),
                HttpStatus.BAD_REQUEST,
                "INVALID_SEARCH_REQUEST");
    }

    @Test
    void shouldMapDocumentNotFoundTo404() {
        assertProblem(
                handler.handleDocumentNotFound(new DocumentNotFoundException(DOCUMENT_ID)),
                HttpStatus.NOT_FOUND,
                "DOCUMENT_NOT_FOUND");
    }

    @Test
    void shouldMapKnowledgeProcessingTo409() {
        assertProblem(
                handler.handleKnowledgeProcessing(new KnowledgeProcessingException("document processing failed")),
                HttpStatus.CONFLICT,
                "KNOWLEDGE_PROCESSING_FAILED");
    }

    private static void assertProblem(ProblemDetail problem, HttpStatus status, String code) {
        assertEquals(status.value(), problem.getStatus());
        assertEquals(status.getReasonPhrase(), problem.getTitle());
        assertEquals(code, problem.getProperties().get("code"));
    }
}
