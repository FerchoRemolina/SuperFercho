CREATE SCHEMA IF NOT EXISTS knowledge;

CREATE TABLE knowledge.documents (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    source VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_knowledge_documents_status CHECK (
        status IN ('RECEIVED', 'CHUNKED', 'READY', 'FAILED', 'INACTIVE')
    )
);

CREATE TABLE knowledge.document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    position INTEGER NOT NULL,
    text TEXT NOT NULL,
    embedded BOOLEAN NOT NULL,
    CONSTRAINT fk_knowledge_document_chunks_document
        FOREIGN KEY (document_id) REFERENCES knowledge.documents (id) ON DELETE CASCADE,
    CONSTRAINT uk_knowledge_document_chunks_document_position UNIQUE (document_id, position),
    CONSTRAINT ck_knowledge_document_chunks_position CHECK (position >= 0),
    CONSTRAINT ck_knowledge_document_chunks_text CHECK (char_length(btrim(text)) > 0)
);

CREATE INDEX idx_knowledge_document_chunks_document_id
    ON knowledge.document_chunks (document_id);
