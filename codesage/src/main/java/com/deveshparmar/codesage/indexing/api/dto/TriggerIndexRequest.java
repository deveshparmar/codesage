package com.deveshparmar.codesage.indexing.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TriggerIndexRequest(
        @NotNull UUID repositoryId,
        String branchName,
        String commitSha,
        boolean fullReindex
) {
}
