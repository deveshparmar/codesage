package com.deveshparmar.codesage.scm.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequestFileResponse(
        @JsonProperty("filename") String filename,
        @JsonProperty("status") String status,
        @JsonProperty("additions") int additions,
        @JsonProperty("deletions") int deletions,
        @JsonProperty("changes") int changes,
        @JsonProperty("patch") String patch
) {
}
