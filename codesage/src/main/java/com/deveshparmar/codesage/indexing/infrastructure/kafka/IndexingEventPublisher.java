package com.deveshparmar.codesage.indexing.infrastructure.kafka;

import com.deveshparmar.codesage.indexing.config.IndexingProperties;
import com.deveshparmar.codesage.platform.config.CodeSageProperties;
import com.deveshparmar.codesage.platform.infrastructure.kafka.EmbeddingGenerationRequestedPayload;
import com.deveshparmar.codesage.platform.infrastructure.kafka.EventEnvelope;
import com.deveshparmar.codesage.platform.infrastructure.kafka.RepositoryIndexCompletedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CodeSageProperties codeSageProperties;
    private final IndexingProperties indexingProperties;

    public void publishRepositoryIndexCompleted(UUID organizationId, UUID correlationId, RepositoryIndexCompletedPayload payload) {
        publish(
                codeSageProperties.getKafka().getTopics().getRepositoryIndexCompleted(),
                organizationId,
                correlationId,
                "repository.index.completed",
                payload
        );
    }

    public void publishEmbeddingGenerationRequested(UUID organizationId, UUID correlationId, UUID repositoryId, List<UUID> chunkIds, String model) {
        int batchSize = indexingProperties.getEmbeddingBatchSize();
        for (int index = 0; index < chunkIds.size(); index += batchSize) {
            List<UUID> batch = chunkIds.subList(index, Math.min(index + batchSize, chunkIds.size()));
            publish(
                    codeSageProperties.getKafka().getTopics().getEmbeddingGenerationRequested(),
                    organizationId,
                    correlationId,
                    "embedding.generation.requested",
                    new EmbeddingGenerationRequestedPayload(repositoryId, batch, model)
            );
        }
    }

    private void publish(String topic, UUID organizationId, UUID correlationId, String eventType, Object payload) {
        EventEnvelope<Object> envelope = EventEnvelope.create(eventType, organizationId, correlationId, payload);
        kafkaTemplate.send(topic, organizationId.toString(), envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} to {}", eventType, topic, ex);
                    } else {
                        log.info("Published {} eventId={}", eventType, envelope.eventId());
                    }
                });
    }
}
