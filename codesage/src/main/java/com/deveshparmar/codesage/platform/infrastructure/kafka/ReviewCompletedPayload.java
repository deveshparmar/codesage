package com.deveshparmar.codesage.platform.infrastructure.kafka;

import java.util.UUID;

public record ReviewCompletedPayload(
        UUID reviewId,
        UUID pullRequestId,
        UUID repositoryId,
        String status,
        int findingsCount,
        int commentsPosted,
        long durationMs
) {
}
