package com.deveshparmar.codesage.scm.domain;

import com.deveshparmar.codesage.common.domain.ScmProviderType;

import java.time.Instant;

public record ScmRepository(
        String externalId,
        String name,
        String fullName,
        String cloneUrl,
        String defaultBranch,
        boolean isPrivate,
        ScmProviderType provider,
        String htmlUrl,
        Instant updatedAt
) {
}
