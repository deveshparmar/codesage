package com.deveshparmar.codesage.scm.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubCreateReviewRequest(
        @JsonProperty("body") String body,
        @JsonProperty("event") String event,
        @JsonProperty("comments") java.util.List<GitHubReviewCommentInput> comments
) {
}
