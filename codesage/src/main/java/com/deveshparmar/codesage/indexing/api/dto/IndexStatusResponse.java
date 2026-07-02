package com.deveshparmar.codesage.indexing.api.dto;

import com.deveshparmar.codesage.common.domain.IndexingStatus;

import java.time.Instant;
import java.util.UUID;

public record IndexStatusResponse(
        UUID repositoryId,
        IndexingStatus indexingStatus,
        Instant lastIndexedAt,
        String message
) {
}
