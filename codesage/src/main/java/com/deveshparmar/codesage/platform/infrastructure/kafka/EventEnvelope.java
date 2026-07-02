package com.deveshparmar.codesage.platform.infrastructure.kafka;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID correlationId,
        UUID organizationId,
        T payload
) {
    public static <T> EventEnvelope<T> create(String eventType, UUID organizationId, UUID correlationId, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                correlationId != null ? correlationId : UUID.randomUUID(),
                organizationId,
                payload
        );
    }
}
