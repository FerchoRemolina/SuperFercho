package com.superfercho.knowledge.domain.model;

import com.superfercho.knowledge.domain.exception.InvalidDocumentException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class KnowledgeDocument {

    private final DocumentId id;
    private final DocumentTitle title;
    private final DocumentSource source;
    private final DocumentContent content;
    private final DocumentStatus status;
    private final List<KnowledgeChunk> chunks;
    private final Instant createdAt;
    private final Instant updatedAt;

    private KnowledgeDocument(
            DocumentId id,
            DocumentTitle title,
            DocumentSource source,
            DocumentContent content,
            DocumentStatus status,
            List<KnowledgeChunk> chunks,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.content = content;
        this.status = status;
        this.chunks = chunks;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static KnowledgeDocument create(
            DocumentTitle title, DocumentSource source, DocumentContent content, Instant createdAt) {
        return of(
                DocumentId.generate(),
                title,
                source,
                content,
                DocumentStatus.RECEIVED,
                List.of(),
                createdAt,
                createdAt);
    }

    public static KnowledgeDocument reconstitute(
            DocumentId id,
            DocumentTitle title,
            DocumentSource source,
            DocumentContent content,
            DocumentStatus status,
            List<KnowledgeChunk> chunks,
            Instant createdAt,
            Instant updatedAt) {
        return of(id, title, source, content, status, chunks, createdAt, updatedAt);
    }

    public KnowledgeDocument replaceContent(DocumentContent content, Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        requireNonNull(content, "content");
        return of(id, title, source, content, DocumentStatus.RECEIVED, List.of(), createdAt, at);
    }

    public KnowledgeDocument replaceChunks(List<ChunkText> chunkTexts, Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        if (status != DocumentStatus.RECEIVED && status != DocumentStatus.CHUNKED) {
            throw new InvalidDocumentException("chunks can only be replaced when RECEIVED or CHUNKED");
        }
        if (chunkTexts == null || chunkTexts.isEmpty()) {
            throw new InvalidDocumentException("chunks cannot be empty");
        }
        List<KnowledgeChunk> next = new ArrayList<>();
        for (int index = 0; index < chunkTexts.size(); index++) {
            ChunkText text = chunkTexts.get(index);
            requireNonNull(text, "chunk text");
            next.add(KnowledgeChunk.create(ChunkId.generate(), new ChunkPosition(index), text));
        }
        return of(id, title, source, content, DocumentStatus.CHUNKED, next, createdAt, at);
    }

    public KnowledgeDocument markChunkEmbedded(ChunkId chunkId, Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        requireNonNull(chunkId, "chunkId");
        if (status != DocumentStatus.CHUNKED) {
            throw new InvalidDocumentException("chunks can only be marked embedded when CHUNKED");
        }
        boolean found = false;
        List<KnowledgeChunk> next = new ArrayList<>(chunks.size());
        for (KnowledgeChunk chunk : chunks) {
            if (chunk.id().equals(chunkId)) {
                found = true;
                next.add(chunk.markEmbedded());
            } else {
                next.add(chunk);
            }
        }
        if (!found) {
            throw new InvalidDocumentException("chunk not found: " + chunkId.value());
        }
        DocumentStatus nextStatus = allEmbedded(next) ? DocumentStatus.READY : DocumentStatus.CHUNKED;
        return of(id, title, source, content, nextStatus, next, createdAt, at);
    }

    public KnowledgeDocument markFailed(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        if (status != DocumentStatus.RECEIVED && status != DocumentStatus.CHUNKED) {
            throw new InvalidDocumentException("document can only fail when RECEIVED or CHUNKED");
        }
        return of(id, title, source, content, DocumentStatus.FAILED, chunks, createdAt, at);
    }

    public KnowledgeDocument reprocess(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        if (status != DocumentStatus.FAILED) {
            throw new InvalidDocumentException("document can only be reprocessed when FAILED");
        }
        return of(id, title, source, content, DocumentStatus.RECEIVED, List.of(), createdAt, at);
    }

    public KnowledgeDocument deactivate(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        if (status != DocumentStatus.READY) {
            throw new InvalidDocumentException("document can only be deactivated when READY");
        }
        return of(id, title, source, content, DocumentStatus.INACTIVE, chunks, createdAt, at);
    }

    public KnowledgeDocument reactivate(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        if (status != DocumentStatus.INACTIVE) {
            throw new InvalidDocumentException("document can only be reactivated when INACTIVE");
        }
        return of(id, title, source, content, DocumentStatus.READY, chunks, createdAt, at);
    }

    public boolean searchable() {
        return status == DocumentStatus.READY;
    }

    public DocumentId id() {
        return id;
    }

    public DocumentTitle title() {
        return title;
    }

    public DocumentSource source() {
        return source;
    }

    public DocumentContent content() {
        return content;
    }

    public DocumentStatus status() {
        return status;
    }

    public List<KnowledgeChunk> chunks() {
        return chunks;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private Instant requireCurrentTime(Instant currentTime) {
        requireNonNull(currentTime, "currentTime");
        if (currentTime.isBefore(createdAt)) {
            throw new InvalidDocumentException("currentTime must not be before createdAt");
        }
        return currentTime;
    }

    private static KnowledgeDocument of(
            DocumentId id,
            DocumentTitle title,
            DocumentSource source,
            DocumentContent content,
            DocumentStatus status,
            List<KnowledgeChunk> chunks,
            Instant createdAt,
            Instant updatedAt) {
        requireNonNull(id, "id");
        requireNonNull(title, "title");
        requireNonNull(source, "source");
        requireNonNull(content, "content");
        requireNonNull(status, "status");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidDocumentException("createdAt must not be after updatedAt");
        }
        List<KnowledgeChunk> copy = copyChunks(chunks);
        validateChunksForStatus(status, copy);
        return new KnowledgeDocument(id, title, source, content, status, copy, createdAt, updatedAt);
    }

    private static List<KnowledgeChunk> copyChunks(List<KnowledgeChunk> chunks) {
        if (chunks == null) {
            throw new InvalidDocumentException("chunks cannot be null");
        }
        List<KnowledgeChunk> copy = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            requireNonNull(chunk, "chunk");
            copy.add(chunk);
        }
        return List.copyOf(copy);
    }

    private static void validateChunksForStatus(DocumentStatus status, List<KnowledgeChunk> chunks) {
        validatePositions(chunks);
        switch (status) {
            case RECEIVED -> {
                if (!chunks.isEmpty()) {
                    throw new InvalidDocumentException("RECEIVED document cannot have chunks");
                }
            }
            case CHUNKED -> {
                requireAtLeastOneChunk(chunks);
                if (allEmbedded(chunks)) {
                    throw new InvalidDocumentException("CHUNKED document must have at least one chunk without embedding");
                }
            }
            case READY, INACTIVE -> {
                requireAtLeastOneChunk(chunks);
                if (!allEmbedded(chunks)) {
                    throw new InvalidDocumentException(status + " document requires every chunk to be embedded");
                }
            }
            case FAILED -> {
                if (!chunks.isEmpty() && allEmbedded(chunks)) {
                    throw new InvalidDocumentException("FAILED document cannot have every chunk embedded");
                }
            }
        }
    }

    private static void validatePositions(List<KnowledgeChunk> chunks) {
        Set<Integer> positions = new HashSet<>();
        Set<ChunkId> ids = new HashSet<>();
        for (int index = 0; index < chunks.size(); index++) {
            KnowledgeChunk chunk = chunks.get(index);
            if (!ids.add(chunk.id())) {
                throw new InvalidDocumentException("chunk ids must be unique");
            }
            int position = chunk.position().value();
            if (!positions.add(position)) {
                throw new InvalidDocumentException("chunk positions must be unique");
            }
            if (position != index) {
                throw new InvalidDocumentException("chunk positions must be contiguous from 0");
            }
        }
    }

    private static void requireAtLeastOneChunk(List<KnowledgeChunk> chunks) {
        if (chunks.isEmpty()) {
            throw new InvalidDocumentException("document must have at least one chunk");
        }
    }

    private static boolean allEmbedded(List<KnowledgeChunk> chunks) {
        for (KnowledgeChunk chunk : chunks) {
            if (!chunk.embedded()) {
                return false;
            }
        }
        return true;
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidDocumentException(field + " cannot be null");
        }
    }
}
