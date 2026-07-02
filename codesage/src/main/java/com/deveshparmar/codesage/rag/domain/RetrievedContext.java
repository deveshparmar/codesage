package com.deveshparmar.codesage.rag.domain;

import com.deveshparmar.codesage.common.domain.ChunkType;

import java.util.Map;
import java.util.UUID;

public record RetrievedContext(
        UUID chunkId,
        UUID fileId,
        ChunkType chunkType,
        String packageName,
        String className,
        String methodName,
        String filePath,
        int startLine,
        int endLine,
        String content,
        double similarity,
        Map<String, Object> metadata
) {
}
