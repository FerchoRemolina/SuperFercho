package com.superfercho.knowledge.application.usecase;

import com.superfercho.knowledge.application.dto.ChunkEmbedding;
import com.superfercho.knowledge.application.dto.DocumentResult;
import com.superfercho.knowledge.application.dto.EmbeddingVector;
import com.superfercho.knowledge.application.dto.ProcessDocumentCommand;
import com.superfercho.knowledge.application.exception.DocumentNotFoundException;
import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import com.superfercho.knowledge.application.port.ClockPort;
import com.superfercho.knowledge.application.port.DocumentChunkerPort;
import com.superfercho.knowledge.application.port.EmbeddingPort;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.application.port.KnowledgeVectorStorePort;
import com.superfercho.knowledge.domain.exception.InvalidDocumentException;
import com.superfercho.knowledge.domain.model.ChunkText;
import com.superfercho.knowledge.domain.model.DocumentId;
import com.superfercho.knowledge.domain.model.DocumentStatus;
import com.superfercho.knowledge.domain.model.KnowledgeChunk;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProcessDocumentUseCase {

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentChunkerPort chunker;
    private final EmbeddingPort embeddingPort;
    private final KnowledgeVectorStorePort vectorStore;
    private final ClockPort clockPort;

    public ProcessDocumentUseCase(
            KnowledgeDocumentRepository documentRepository,
            DocumentChunkerPort chunker,
            EmbeddingPort embeddingPort,
            KnowledgeVectorStorePort vectorStore,
            ClockPort clockPort) {
        this.documentRepository = documentRepository;
        this.chunker = chunker;
        this.embeddingPort = embeddingPort;
        this.vectorStore = vectorStore;
        this.clockPort = clockPort;
    }

    public DocumentResult execute(ProcessDocumentCommand command) {
        DocumentId documentId = new DocumentId(command.documentId());
        KnowledgeDocument document = documentRepository
                .findById(documentId.value())
                .orElseThrow(() -> new DocumentNotFoundException(documentId.value()));
        Instant now = clockPort.now();
        if (document.status() == DocumentStatus.READY || document.status() == DocumentStatus.INACTIVE) {
            throw new InvalidDocumentException("document can only be processed when RECEIVED, CHUNKED or FAILED");
        }
        try {
            if (document.status() == DocumentStatus.FAILED) {
                document = reprocessFailed(document, now);
            }
            if (document.status() == DocumentStatus.RECEIVED) {
                document = chunkReceived(document, now);
            }
            document = embedPending(document, now);
            return DocumentResult.from(document);
        } catch (InvalidDocumentException exception) {
            throw exception;
        } catch (KnowledgeProcessingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            failIfPossible(document, now);
            throw new KnowledgeProcessingException("document processing failed");
        }
    }

    private KnowledgeDocument reprocessFailed(KnowledgeDocument document, Instant now) {
        KnowledgeDocument received = document.reprocess(now);
        try {
            vectorStore.deleteByDocumentId(document.id().value());
        } catch (KnowledgeProcessingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new KnowledgeProcessingException("failed to delete document embeddings");
        }
        return documentRepository.save(received);
    }

    private KnowledgeDocument chunkReceived(KnowledgeDocument document, Instant now) {
        List<ChunkText> chunkTexts;
        try {
            chunkTexts = chunker.chunk(document.content());
        } catch (KnowledgeProcessingException exception) {
            failIfPossible(document, now);
            throw exception;
        } catch (RuntimeException exception) {
            failIfPossible(document, now);
            throw new KnowledgeProcessingException("failed to chunk document");
        }
        if (chunkTexts == null || chunkTexts.isEmpty()) {
            failIfPossible(document, now);
            throw new KnowledgeProcessingException("chunker produced no chunks");
        }
        try {
            return documentRepository.save(document.replaceChunks(chunkTexts, now));
        } catch (InvalidDocumentException exception) {
            failIfPossible(document, now);
            throw new KnowledgeProcessingException("chunker produced invalid chunks");
        }
    }

    private KnowledgeDocument embedPending(KnowledgeDocument document, Instant now) {
        KnowledgeDocument current = document;
        UUID documentId = current.id().value();
        for (KnowledgeChunk chunk : current.chunks()) {
            if (chunk.embedded()) {
                continue;
            }
            EmbeddingVector embedding = embedChunk(chunk.text().value());
            try {
                vectorStore.upsert(
                        new ChunkEmbedding(documentId, chunk.id().value(), chunk.position().value(), embedding));
            } catch (KnowledgeProcessingException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw new KnowledgeProcessingException("failed to store chunk embedding");
            }
            current = documentRepository.save(current.markChunkEmbedded(chunk.id(), now));
        }
        return current;
    }

    private EmbeddingVector embedChunk(String text) {
        List<EmbeddingVector> embeddings;
        try {
            embeddings = embeddingPort.embed(List.of(text));
        } catch (KnowledgeProcessingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new KnowledgeProcessingException("failed to generate embedding");
        }
        if (embeddings == null || embeddings.size() != 1) {
            throw new KnowledgeProcessingException("embedding provider returned an invalid result");
        }
        return embeddings.get(0);
    }

    private void failIfPossible(KnowledgeDocument document, Instant now) {
        if (document.status() != DocumentStatus.RECEIVED && document.status() != DocumentStatus.CHUNKED) {
            return;
        }
        documentRepository.save(document.markFailed(now));
    }
}
