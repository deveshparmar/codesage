package com.deveshparmar.codesage.indexing.domain;

import java.util.List;
import java.util.UUID;

public record IndexingResult(
        UUID repositoryId,
        String branchName,
        String commitSha,
        int filesIndexed,
        int chunksCreated,
        int embeddingsQueued,
        long durationMs,
        IndexingStatus status,
        List<UUID> chunkIdsForEmbedding
) {
    public enum IndexingStatus {
        SUCCESS,
        PARTIAL,
        FAILED
    }
}
