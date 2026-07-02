package com.deveshparmar.codesage.rag.api.dto;

import com.deveshparmar.codesage.common.domain.ChunkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SemanticSearchRequest(
        @NotNull UUID repositoryId,
        @NotBlank String query,
        Integer topK,
        Double minSimilarity,
        ChunkType chunkType,
        String packageName,
        String className,
        String filePath
) {
}
