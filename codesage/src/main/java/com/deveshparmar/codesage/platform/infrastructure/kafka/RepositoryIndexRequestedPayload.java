package com.deveshparmar.codesage.platform.infrastructure.kafka;

import java.util.UUID;

public record RepositoryIndexRequestedPayload(
        UUID repositoryId,
        String branchName,
        String commitSha,
        boolean fullReindex
) {
}
