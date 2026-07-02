package com.deveshparmar.codesage.scm.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepositoryResponse(
        @JsonProperty("id") long id,
        @JsonProperty("name") String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("clone_url") String cloneUrl,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("default_branch") String defaultBranch,
        @JsonProperty("private") boolean isPrivate,
        @JsonProperty("updated_at") String updatedAt
) {
}
