package com.deveshparmar.codesage.review.domain;

import com.deveshparmar.codesage.common.domain.ChunkType;

public record ModifiedMethod(
        String filePath,
        String className,
        String methodName,
        ChunkType chunkType,
        int startLine,
        int endLine,
        String sourceCode,
        String patch
) {
    public String signature() {
        if (methodName == null || methodName.isBlank()) {
            return className;
        }
        return className + "#" + methodName;
    }
}
