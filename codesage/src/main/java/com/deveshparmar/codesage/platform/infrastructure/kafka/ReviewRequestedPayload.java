package com.deveshparmar.codesage.platform.infrastructure.kafka;

import java.util.UUID;

public record ReviewRequestedPayload(
        UUID reviewId,
        UUID pullRequestId,
        UUID repositoryId,
        int pullRequestNumber,
        String headSha,
        String baseSha,
        String triggerSource
) {
}
