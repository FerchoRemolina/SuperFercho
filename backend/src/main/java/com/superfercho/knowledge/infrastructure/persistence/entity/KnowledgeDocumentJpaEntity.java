package com.superfercho.knowledge.infrastructure.persistence.entity;

import com.superfercho.knowledge.domain.model.DocumentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documents", schema = "knowledge")
public class KnowledgeDocumentJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "document_id", nullable = false)
    @OrderBy("position ASC")
    private List<KnowledgeChunkJpaEntity> chunks = new ArrayList<>();

    protected KnowledgeDocumentJpaEntity() {
    }

    public KnowledgeDocumentJpaEntity(
            UUID id,
            String title,
            String source,
            String content,
            DocumentStatus status,
            Instant createdAt,
            Instant updatedAt,
            List<KnowledgeChunkJpaEntity> chunks) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.chunks = chunks == null ? new ArrayList<>() : new ArrayList<>(chunks);
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getContent() {
        return content;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<KnowledgeChunkJpaEntity> getChunks() {
        return chunks;
    }

    public void overwriteScalarsFrom(KnowledgeDocumentJpaEntity source) {
        this.title = source.getTitle();
        this.source = source.getSource();
        this.content = source.getContent();
        this.status = source.getStatus();
        this.createdAt = source.getCreatedAt();
        this.updatedAt = source.getUpdatedAt();
    }
}
