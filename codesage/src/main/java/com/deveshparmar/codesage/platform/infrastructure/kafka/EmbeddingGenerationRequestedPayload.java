package com.deveshparmar.codesage.platform.infrastructure.kafka;

import java.util.List;
import java.util.UUID;

public record EmbeddingGenerationRequestedPayload(
        UUID repositoryId,
        List<UUID> chunkIds,
        String model
) {
}
