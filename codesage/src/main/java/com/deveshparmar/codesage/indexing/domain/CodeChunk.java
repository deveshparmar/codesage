package com.deveshparmar.codesage.indexing.domain;

import com.deveshparmar.codesage.common.domain.ChunkType;

import java.util.Map;
import java.util.UUID;

public record CodeChunk(
        ChunkType chunkType,
        String chunkHash,
        String packageName,
        String className,
        String methodName,
        int startLine,
        int endLine,
        String content,
        Map<String, Object> metadata
) {
    public static CodeChunk of(
            ChunkType chunkType,
            String chunkHash,
            String packageName,
            String className,
            String methodName,
            int startLine,
            int endLine,
            String content,
            UUID repositoryId,
            String branchName,
            String commitSha,
            String filePath,
            String language
    ) {
        Map<String, Object> metadata = Map.of(
                "repositoryId", repositoryId.toString(),
                "branch", branchName,
                "commitSha", commitSha,
                "file", filePath,
                "language", language,
                "className", className != null ? className : "",
                "methodName", methodName != null ? methodName : ""
        );
        return new CodeChunk(chunkType, chunkHash, packageName, className, methodName, startLine, endLine, content, metadata);
    }
}
