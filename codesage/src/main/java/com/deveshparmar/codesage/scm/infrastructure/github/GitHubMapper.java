package com.deveshparmar.codesage.scm.infrastructure.github;

import com.deveshparmar.codesage.common.domain.ScmProviderType;
import com.deveshparmar.codesage.scm.domain.ScmRepository;
import com.deveshparmar.codesage.scm.infrastructure.github.dto.GitHubRepositoryResponse;

import java.time.Instant;

final class GitHubMapper {

    private GitHubMapper() {
    }

    static ScmRepository toScmRepository(GitHubRepositoryResponse response) {
        return new ScmRepository(
                String.valueOf(response.id()),
                response.name(),
                response.fullName(),
                response.cloneUrl(),
                response.defaultBranch(),
                response.isPrivate(),
                ScmProviderType.GITHUB,
                response.htmlUrl(),
                parseInstant(response.updatedAt())
        );
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        return Instant.parse(value);
    }
}
