package com.deveshparmar.codesage.rag.infrastructure.pgvector;

import com.deveshparmar.codesage.common.domain.ChunkType;
import com.deveshparmar.codesage.llm.config.EmbeddingProperties;
import com.deveshparmar.codesage.rag.domain.RetrievedContext;
import com.deveshparmar.codesage.rag.domain.SimilaritySearchQuery;
import com.deveshparmar.codesage.rag.domain.VectorSearchPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PgVectorSearchRepository implements VectorSearchPort {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final EmbeddingProperties embeddingProperties;

    @Override
    @SuppressWarnings("unchecked")
    public List<RetrievedContext> search(SimilaritySearchQuery query) {
        int dimensions = embeddingProperties.getDimensions();
        String halfvecCast = "halfvec(" + dimensions + ")";
        StringBuilder sql = new StringBuilder("""
                SELECT c.id AS chunk_id,
                       c.file_id,
                       c.chunk_type,
                       c.package_name,
                       c.class_name,
                       c.method_name,
                       c.start_line,
                       c.end_line,
                       c.content,
                       c.metadata,
                       f.path AS file_path,
                       1 - (e.embedding::%s <=> CAST(:queryVector AS %s)) AS similarity
                FROM chunks c
                JOIN embeddings e ON e.chunk_id = c.id
                JOIN files f ON f.id = c.file_id
                JOIN branches b ON b.id = f.branch_id
                WHERE b.repository_id = :repositoryId
                """.formatted(halfvecCast, halfvecCast));

        if (query.chunkType() != null) {
            sql.append(" AND c.chunk_type = :chunkType");
        }
        if (query.packageName() != null && !query.packageName().isBlank()) {
            sql.append(" AND c.package_name = :packageName");
        }
        if (query.className() != null && !query.className().isBlank()) {
            sql.append(" AND c.class_name = :className");
        }
        if (query.filePath() != null && !query.filePath().isBlank()) {
            sql.append(" AND f.path = :filePath");
        }

        sql.append("""
                 AND 1 - (e.embedding::%s <=> CAST(:queryVector AS %s)) >= :minSimilarity
                ORDER BY e.embedding::%s <=> CAST(:queryVector AS %s)
                LIMIT :topK
                """.formatted(halfvecCast, halfvecCast, halfvecCast, halfvecCast));

        var nativeQuery = entityManager.createNativeQuery(sql.toString())
                .setParameter("repositoryId", query.repositoryId())
                .setParameter("queryVector", new PGvector(query.queryVector()).toString())
                .setParameter("minSimilarity", query.minSimilarity())
                .setParameter("topK", query.topK());

        if (query.chunkType() != null) {
            nativeQuery.setParameter("chunkType", query.chunkType().name());
        }
        if (query.packageName() != null && !query.packageName().isBlank()) {
            nativeQuery.setParameter("packageName", query.packageName());
        }
        if (query.className() != null && !query.className().isBlank()) {
            nativeQuery.setParameter("className", query.className());
        }
        if (query.filePath() != null && !query.filePath().isBlank()) {
            nativeQuery.setParameter("filePath", query.filePath());
        }

        List<Object[]> rows = nativeQuery.getResultList();
        List<RetrievedContext> results = new ArrayList<>();

        for (Object[] row : rows) {
            results.add(mapRow(row));
        }
        return results;
    }

    private RetrievedContext mapRow(Object[] row) {
        Map<String, Object> metadata = parseMetadata(row[9]);
        return new RetrievedContext(
                asUuid(row[0]),
                asUuid(row[1]),
                ChunkType.valueOf(row[2].toString()),
                row[3] != null ? row[3].toString() : null,
                row[4] != null ? row[4].toString() : null,
                row[5] != null ? row[5].toString() : null,
                row[10].toString(),
                ((Number) row[6]).intValue(),
                ((Number) row[7]).intValue(),
                row[8].toString(),
                ((Number) row[11]).doubleValue(),
                metadata
        );
    }

    private Map<String, Object> parseMetadata(Object value) {
        if (value == null) {
            return Map.of();
        }
        try {
            if (value instanceof Map<?, ?> map) {
                return objectMapper.convertValue(map, new TypeReference<>() {});
            }
            return objectMapper.readValue(value.toString(), new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private UUID asUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }
}
