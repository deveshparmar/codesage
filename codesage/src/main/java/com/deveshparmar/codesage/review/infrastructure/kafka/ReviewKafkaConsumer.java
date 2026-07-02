package com.deveshparmar.codesage.review.infrastructure.kafka;

import com.deveshparmar.codesage.indexing.infrastructure.redis.KafkaEventIdempotencyService;
import com.deveshparmar.codesage.platform.infrastructure.kafka.EventEnvelope;
import com.deveshparmar.codesage.platform.infrastructure.kafka.ReviewRequestedPayload;
import com.deveshparmar.codesage.review.application.PullRequestReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewKafkaConsumer {

    private final KafkaEventIdempotencyService idempotencyService;
    private final PullRequestReviewService pullRequestReviewService;

    @KafkaListener(
            topics = "${codesage.kafka.topics.review-requested}",
            groupId = "codesage-review"
    )
    public void onReviewRequested(EventEnvelope<ReviewRequestedPayload> envelope) {
        if (idempotencyService.alreadyProcessed(envelope.eventId())) {
            log.info("Skipping duplicate review event {}", envelope.eventId());
            return;
        }
        log.info("Processing review event {} for review {}", envelope.eventId(), envelope.payload().reviewId());
        try {
            pullRequestReviewService.executeReview(
                    envelope.organizationId(),
                    envelope.correlationId(),
                    envelope.payload()
            );
        } catch (Exception ex) {
            idempotencyService.release(envelope.eventId());
            throw ex;
        }
    }
}
