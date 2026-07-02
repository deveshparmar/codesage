package com.deveshparmar.codesage.scm.domain;

import com.deveshparmar.codesage.common.domain.PullRequestState;

import java.time.Instant;

public record ScmPullRequest(
        String externalId,
        int number,
        String title,
        String sourceBranch,
        String targetBranch,
        String headSha,
        String baseSha,
        PullRequestState state,
        String author,
        String htmlUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
