package com.deveshparmar.codesage.platform.infrastructure.kafka;

import java.util.UUID;

public record RepositoryIndexCompletedPayload(
        UUID repositoryId,
        String branchName,
        String commitSha,
        int filesIndexed,
        int chunksCreated,
        int embeddingsQueued,
        long durationMs,
        String status
) {
}
