package com.deveshparmar.codesage.indexing.infrastructure.persistence;

import com.deveshparmar.codesage.common.exception.InvalidRequestException;
import com.deveshparmar.codesage.llm.config.EmbeddingProperties;
import com.pgvector.PGvector;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EmbeddingVectorRepository {

    private final EntityManager entityManager;
    private final EmbeddingProperties embeddingProperties;

    @Transactional
    public void upsertEmbedding(UUID chunkId, float[] vector, String model) {
        validateVector(vector);
        UUID embeddingId = UUID.randomUUID();
        entityManager.createNativeQuery("""
                        INSERT INTO embeddings (id, chunk_id, embedding, model, created_at)
                        VALUES (:id, :chunkId, CAST(:embedding AS vector), :model, :createdAt)
                        ON CONFLICT (chunk_id) DO UPDATE
                        SET embedding = EXCLUDED.embedding,
                            model = EXCLUDED.model,
                            created_at = EXCLUDED.created_at
                        """)
                .setParameter("id", embeddingId)
                .setParameter("chunkId", chunkId)
                .setParameter("embedding", new PGvector(vector).toString())
                .setParameter("model", model)
                .setParameter("createdAt", Instant.now())
                .executeUpdate();
    }

    private void validateVector(float[] vector) {
        if (vector.length != embeddingProperties.getDimensions()) {
            throw new InvalidRequestException(
                    "Embedding vector length " + vector.length
                            + " does not match configured dimensions "
                            + embeddingProperties.getDimensions()
            );
        }
    }
}
