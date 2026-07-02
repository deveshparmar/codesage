package com.deveshparmar.codesage.scm.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubCreateReviewCommentRequest(
        @JsonProperty("body") String body,
        @JsonProperty("commit_id") String commitId,
        @JsonProperty("path") String path,
        @JsonProperty("line") int line,
        @JsonProperty("side") String side
) {
}
