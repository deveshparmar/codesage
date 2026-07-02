package com.deveshparmar.codesage.review.infrastructure.kafka;

import com.deveshparmar.codesage.platform.config.CodeSageProperties;
import com.deveshparmar.codesage.platform.infrastructure.kafka.EventEnvelope;
import com.deveshparmar.codesage.platform.infrastructure.kafka.ReviewCompletedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CodeSageProperties codeSageProperties;

    public void publishReviewCompleted(UUID organizationId, UUID correlationId, ReviewCompletedPayload payload) {
        EventEnvelope<Object> envelope = EventEnvelope.create(
                "review.completed",
                organizationId,
                correlationId,
                payload
        );
        kafkaTemplate.send(
                codeSageProperties.getKafka().getTopics().getReviewCompleted(),
                organizationId.toString(),
                envelope
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish review.completed for review {}", payload.reviewId(), ex);
            } else {
                log.info("Published review.completed for review {}", payload.reviewId());
            }
        });
    }
}
