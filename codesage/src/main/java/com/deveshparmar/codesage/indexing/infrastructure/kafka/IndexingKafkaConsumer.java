package com.deveshparmar.codesage.indexing.infrastructure.kafka;

import com.deveshparmar.codesage.indexing.application.EmbeddingGenerationService;
import com.deveshparmar.codesage.indexing.application.RepositoryIndexingService;
import com.deveshparmar.codesage.indexing.infrastructure.redis.KafkaEventIdempotencyService;
import com.deveshparmar.codesage.platform.infrastructure.kafka.EmbeddingGenerationRequestedPayload;
import com.deveshparmar.codesage.platform.infrastructure.kafka.EventEnvelope;
import com.deveshparmar.codesage.platform.infrastructure.kafka.RepositoryIndexRequestedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingKafkaConsumer {

    private final KafkaEventIdempotencyService idempotencyService;
    private final RepositoryIndexingService repositoryIndexingService;
    private final EmbeddingGenerationService embeddingGenerationService;

    @KafkaListener(
            topics = "${codesage.kafka.topics.repository-index-requested}",
            groupId = "codesage-indexing"
    )
    public void onRepositoryIndexRequested(EventEnvelope<RepositoryIndexRequestedPayload> envelope) {
        if (idempotencyService.alreadyProcessed(envelope.eventId())) {
            log.info("Skipping duplicate repository index event {}", envelope.eventId());
            return;
        }
        log.info("Processing repository index event {} for repository {}", envelope.eventId(), envelope.payload().repositoryId());
        repositoryIndexingService.indexRepository(envelope.organizationId(), envelope.correlationId(), envelope.payload());
    }

    @KafkaListener(
            topics = "${codesage.kafka.topics.embedding-generation-requested}",
            groupId = "codesage-indexing"
    )
    public void onEmbeddingGenerationRequested(EventEnvelope<EmbeddingGenerationRequestedPayload> envelope) {
        if (idempotencyService.alreadyProcessed(envelope.eventId())) {
            log.info("Skipping duplicate embedding generation event {}", envelope.eventId());
            return;
        }

        EmbeddingGenerationRequestedPayload payload = envelope.payload();
        embeddingGenerationService.generateEmbeddings(payload.repositoryId(), payload);
    }
}
