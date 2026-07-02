package com.deveshparmar.codesage.platform.api.dto;

import com.deveshparmar.codesage.common.domain.ScmProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRepositoryRequest(
        @NotNull ScmProviderType provider,
        @NotBlank String owner,
        @NotBlank String repositoryName,
        @NotBlank String accessToken,
        String webhookSecret
) {
}
