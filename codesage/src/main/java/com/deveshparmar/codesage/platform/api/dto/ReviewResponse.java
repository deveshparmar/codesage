package com.deveshparmar.codesage.platform.api.dto;

import com.deveshparmar.codesage.common.domain.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID pullRequestId,
        ReviewStatus status,
        String summary,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt
) {
}
