package com.deveshparmar.codesage.platform.api.dto;

import java.util.UUID;

public record ApiKeyResponse(
        UUID apiKeyId,
        String apiKey,
        String prefix,
        String message
) {
}
