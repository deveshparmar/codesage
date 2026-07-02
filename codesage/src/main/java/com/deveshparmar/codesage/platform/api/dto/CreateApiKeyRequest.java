package com.deveshparmar.codesage.platform.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateApiKeyRequest(
        @NotBlank String name
) {
}
