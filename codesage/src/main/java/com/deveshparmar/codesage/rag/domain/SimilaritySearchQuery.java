package com.deveshparmar.codesage.rag.domain;

import com.deveshparmar.codesage.common.domain.ChunkType;

import java.util.UUID;

public record SimilaritySearchQuery(
        UUID repositoryId,
        float[] queryVector,
        int topK,
        double minSimilarity,
        ChunkType chunkType,
        String packageName,
        String className,
        String filePath
) {
}
