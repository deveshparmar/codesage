-- Reconfigure pgvector storage for the configured embedding dimensions.
-- Existing embeddings are cleared because vector width changes require a full reindex.
DELETE FROM embeddings;

DROP INDEX IF EXISTS idx_embeddings_hnsw;

ALTER TABLE embeddings
    ALTER COLUMN embedding TYPE vector(${embedding_dimensions});

CREATE INDEX idx_embeddings_hnsw
    ON embeddings USING hnsw ((embedding::halfvec(${embedding_dimensions})) halfvec_cosine_ops);
