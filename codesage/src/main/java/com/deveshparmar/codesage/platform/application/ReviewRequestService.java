package com.deveshparmar.codesage.platform.application;

import com.deveshparmar.codesage.common.domain.ReviewStatus;
import com.deveshparmar.codesage.common.exception.InvalidRequestException;
import com.deveshparmar.codesage.common.exception.ResourceNotFoundException;
import com.deveshparmar.codesage.platform.infrastructure.kafka.ReviewRequestedPayload;
import com.deveshparmar.codesage.platform.infrastructure.kafka.PlatformEventPublisher;
import com.deveshparmar.codesage.platform.infrastructure.persistence.PullRequestEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.PullRequestJpaRepository;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.ReviewEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.ReviewJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewRequestService {

    private final PullRequestJpaRepository pullRequestRepository;
    private final ReviewJpaRepository reviewRepository;
    private final RepositoryService repositoryService;
    private final PlatformEventPublisher eventPublisher;
    private final AuditService auditService;

    @Transactional
    public ReviewEntity requestReview(RepositoryEntity repository, PullRequestEntity pullRequest, String triggerSource, UUID correlationId) {
        ReviewEntity review = new ReviewEntity();
        review.setPullRequestId(pullRequest.getId());
        review.setStatus(ReviewStatus.PENDING);
        reviewRepository.save(review);

        eventPublisher.publishReviewRequested(
                repository.getOrganizationId(),
                correlationId,
                new ReviewRequestedPayload(
                        review.getId(),
                        pullRequest.getId(),
                        repository.getId(),
                        pullRequest.getNumber(),
                        pullRequest.getHeadSha(),
                        pullRequest.getBaseSha(),
                        triggerSource
                )
        );

        auditService.record(
                repository.getOrganizationId(),
                correlationId,
                "REVIEW_REQUESTED",
                "platform-gateway",
                "REVIEW",
                review.getId().toString(),
                java.util.Map.of(
                        "pullRequestNumber", pullRequest.getNumber(),
                        "repository", repository.getFullName(),
                        "triggerSource", triggerSource
                ),
                null
        );

        return review;
    }

    @Transactional
    public ReviewEntity requestReviewByPullRequestId(UUID organizationId, UUID pullRequestId) {
        PullRequestEntity pullRequest = pullRequestRepository.findById(pullRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("PullRequest", pullRequestId.toString()));
        RepositoryEntity repository = repositoryService.getById(pullRequest.getRepositoryId());
        if (!repository.getOrganizationId().equals(organizationId)) {
            throw new InvalidRequestException("Pull request does not belong to organization");
        }
        return requestReview(repository, pullRequest, "API", UUID.randomUUID());
    }

    @Transactional(readOnly = true)
    public List<ReviewEntity> listReviews(UUID pullRequestId) {
        return reviewRepository.findByPullRequestIdOrderByCreatedAtDesc(pullRequestId);
    }

    @Transactional(readOnly = true)
    public ReviewEntity getReview(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId.toString()));
    }
}
