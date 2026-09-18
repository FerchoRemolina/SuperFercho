package com.superfercho.knowledge.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "document_chunks", schema = "knowledge")
public class KnowledgeChunkJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "embedded", nullable = false)
    private boolean embedded;

    protected KnowledgeChunkJpaEntity() {
    }

    public KnowledgeChunkJpaEntity(UUID id, int position, String text, boolean embedded) {
        this.id = id;
        this.position = position;
        this.text = text;
        this.embedded = embedded;
    }

    public UUID getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getText() {
        return text;
    }

    public boolean isEmbedded() {
        return embedded;
    }
}
