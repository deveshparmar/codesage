package com.deveshparmar.codesage.scm.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRef(
        @JsonProperty("ref") String ref,
        @JsonProperty("sha") String sha
) {
}
