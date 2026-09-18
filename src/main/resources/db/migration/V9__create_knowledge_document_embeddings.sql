CREATE TABLE knowledge.document_embeddings (
    chunk_id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    position INTEGER NOT NULL,
    embedding vector(1536) NOT NULL,
    CONSTRAINT fk_knowledge_document_embeddings_document
        FOREIGN KEY (document_id) REFERENCES knowledge.documents (id) ON DELETE CASCADE,
    CONSTRAINT ck_knowledge_document_embeddings_position CHECK (position >= 0)
);

CREATE INDEX idx_knowledge_document_embeddings_document_id
    ON knowledge.document_embeddings (document_id);

CREATE INDEX idx_knowledge_document_embeddings_vector
    ON knowledge.document_embeddings
    USING hnsw (embedding vector_cosine_ops);
