package com.deveshparmar.codesage.platform.infrastructure.kafka;

import com.deveshparmar.codesage.platform.config.CodeSageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CodeSageProperties codeSageProperties;

    public void publishReviewRequested(UUID organizationId, UUID correlationId, ReviewRequestedPayload payload) {
        publish(
                codeSageProperties.getKafka().getTopics().getReviewRequested(),
                organizationId,
                correlationId,
                "review.requested",
                payload
        );
    }

    public void publishRepositoryIndexRequested(UUID organizationId, UUID correlationId, RepositoryIndexRequestedPayload payload) {
        publish(
                codeSageProperties.getKafka().getTopics().getRepositoryIndexRequested(),
                organizationId,
                correlationId,
                "repository.index.requested",
                payload
        );
    }

    public void publishAuditEvent(UUID organizationId, UUID correlationId, AuditEventPayload payload) {
        publish(
                codeSageProperties.getKafka().getTopics().getAuditEvents(),
                organizationId,
                correlationId,
                payload.eventType(),
                payload
        );
    }

    private void publish(String topic, UUID organizationId, UUID correlationId, String eventType, Object payload) {
        EventEnvelope<Object> envelope = EventEnvelope.create(eventType, organizationId, correlationId, payload);
        String partitionKey = organizationId.toString();
        kafkaTemplate.send(topic, partitionKey, envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} to topic {}", eventType, topic, ex);
                    } else {
                        log.info("Published event {} to topic {} with eventId={}", eventType, topic, envelope.eventId());
                    }
                });
    }
}
