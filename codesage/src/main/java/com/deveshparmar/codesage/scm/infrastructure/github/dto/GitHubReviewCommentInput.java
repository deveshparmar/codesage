package com.deveshparmar.codesage.scm.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubReviewCommentInput(
        @JsonProperty("path") String path,
        @JsonProperty("line") int line,
        @JsonProperty("body") String body,
        @JsonProperty("side") String side
) {
}
