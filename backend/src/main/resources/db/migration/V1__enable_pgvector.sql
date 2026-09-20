-- Phase 0 infrastructure only: enable pgvector on the shared PostgreSQL database.
-- No business tables are created in this migration.
CREATE EXTENSION IF NOT EXISTS vector;
