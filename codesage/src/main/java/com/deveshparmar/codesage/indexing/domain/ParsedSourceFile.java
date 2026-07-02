package com.deveshparmar.codesage.indexing.domain;

import java.nio.file.Path;
import java.util.List;

public record ParsedSourceFile(
        Path relativePath,
        String content,
        String contentHash,
        String language,
        List<CodeChunk> chunks
) {
}
