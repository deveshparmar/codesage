package com.deveshparmar.codesage.scm.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubReviewCommentResponse(
        @JsonProperty("id") long id,
        @JsonProperty("path") String path,
        @JsonProperty("line") int line,
        @JsonProperty("body") String body,
        @JsonProperty("created_at") String createdAt
) {
}
