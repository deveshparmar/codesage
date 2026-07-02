package com.deveshparmar.codesage.platform.api.dto;

import com.deveshparmar.codesage.common.domain.Severity;

import java.time.Instant;
import java.util.UUID;

public record ReviewCommentResponse(
        UUID id,
        UUID reviewId,
        String filePath,
        int startLine,
        int endLine,
        Severity severity,
        String category,
        String message,
        String suggestion,
        String externalCommentId,
        Instant createdAt
) {
}
