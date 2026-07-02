package com.deveshparmar.codesage.scm.infrastructure.github;

import com.deveshparmar.codesage.common.domain.PullRequestState;
import com.deveshparmar.codesage.scm.domain.ScmPullRequest;
import com.deveshparmar.codesage.scm.infrastructure.github.dto.GitHubPullRequestResponse;

import java.time.Instant;

final class GitHubPullRequestMapper {

    private GitHubPullRequestMapper() {
    }

    static ScmPullRequest toScmPullRequest(GitHubPullRequestResponse response) {
        return new ScmPullRequest(
                String.valueOf(response.id()),
                response.number(),
                response.title(),
                stripRefPrefix(response.head().ref()),
                stripRefPrefix(response.base().ref()),
                response.head().sha(),
                response.base().sha(),
                mapState(response.state()),
                response.user() != null ? response.user().login() : "unknown",
                response.htmlUrl(),
                parseInstant(response.createdAt()),
                parseInstant(response.updatedAt())
        );
    }

    private static PullRequestState mapState(String state) {
        return switch (state.toLowerCase()) {
            case "open" -> PullRequestState.OPEN;
            case "closed" -> PullRequestState.CLOSED;
            default -> PullRequestState.CLOSED;
        };
    }

    private static String stripRefPrefix(String ref) {
        if (ref != null && ref.startsWith("refs/heads/")) {
            return ref.substring("refs/heads/".length());
        }
        return ref;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        return Instant.parse(value);
    }
}
