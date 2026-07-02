package com.deveshparmar.codesage.platform.application;

import com.deveshparmar.codesage.platform.infrastructure.kafka.AuditEventPayload;
import com.deveshparmar.codesage.platform.infrastructure.kafka.PlatformEventPublisher;
import com.deveshparmar.codesage.platform.infrastructure.persistence.AuditLogEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogJpaRepository auditLogRepository;
    private final PlatformEventPublisher eventPublisher;

    @Transactional
    public void record(
            UUID organizationId,
            UUID correlationId,
            String eventType,
            String actor,
            String resourceType,
            String resourceId,
            Map<String, Object> metadata,
            String ipAddress
    ) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setOrganizationId(organizationId);
        entity.setEventType(eventType);
        entity.setActor(actor);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setMetadata(metadata != null ? metadata : Map.of());
        entity.setIpAddress(ipAddress);
        auditLogRepository.save(entity);

        eventPublisher.publishAuditEvent(
                organizationId,
                correlationId,
                AuditEventPayload.of(organizationId, eventType, actor, resourceType, resourceId, metadata, ipAddress)
        );
    }
}
