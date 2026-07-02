package com.deveshparmar.codesage.platform.api.dto;

import com.deveshparmar.codesage.common.domain.IndexingStatus;
import com.deveshparmar.codesage.common.domain.ScmProviderType;

import java.time.Instant;
import java.util.UUID;

public record RepositoryResponse(
        UUID id,
        UUID organizationId,
        ScmProviderType scmProvider,
        String externalId,
        String name,
        String fullName,
        String defaultBranch,
        boolean isPrivate,
        IndexingStatus indexingStatus,
        Instant lastIndexedAt,
        Instant createdAt
) {
}
