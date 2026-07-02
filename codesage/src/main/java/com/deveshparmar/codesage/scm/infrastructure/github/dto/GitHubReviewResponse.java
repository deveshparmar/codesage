package com.deveshparmar.codesage.scm.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubReviewResponse(
        @JsonProperty("id") long id,
        @JsonProperty("body") String body,
        @JsonProperty("state") String state,
        @JsonProperty("submitted_at") String submittedAt
) {
}
