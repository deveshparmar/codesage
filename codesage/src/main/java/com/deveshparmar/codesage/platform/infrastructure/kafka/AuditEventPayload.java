package com.deveshparmar.codesage.platform.infrastructure.kafka;

import java.util.Map;
import java.util.UUID;

public record AuditEventPayload(
        String eventType,
        String actor,
        String resourceType,
        String resourceId,
        Map<String, Object> metadata,
        String ipAddress
) {
    public static AuditEventPayload of(
            UUID organizationId,
            String eventType,
            String actor,
            String resourceType,
            String resourceId,
            Map<String, Object> metadata,
            String ipAddress
    ) {
        return new AuditEventPayload(eventType, actor, resourceType, resourceId, metadata, ipAddress);
    }
}
