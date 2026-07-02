package com.deveshparmar.codesage.platform.api;

import com.deveshparmar.codesage.platform.api.dto.ReviewCommentResponse;
import com.deveshparmar.codesage.platform.api.dto.ReviewResponse;
import com.deveshparmar.codesage.platform.application.ReviewRequestService;
import com.deveshparmar.codesage.platform.infrastructure.persistence.ReviewEntity;
import com.deveshparmar.codesage.platform.infrastructure.redis.RateLimitService;
import com.deveshparmar.codesage.review.application.ReviewCommentQueryService;
import com.deveshparmar.codesage.review.infrastructure.persistence.ReviewCommentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pull-requests/{pullRequestId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRequestService reviewRequestService;
    private final ReviewCommentQueryService reviewCommentQueryService;
    private final RateLimitService rateLimitService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ReviewResponse requestReview(@PathVariable UUID pullRequestId) {
        UUID organizationId = SecurityContextHelper.getAuthenticatedOrganizationId();
        rateLimitService.checkRateLimit(organizationId);
        ReviewEntity review = reviewRequestService.requestReviewByPullRequestId(organizationId, pullRequestId);
        return toResponse(review);
    }

    @GetMapping
    public List<ReviewResponse> listReviews(@PathVariable UUID pullRequestId) {
        UUID organizationId = SecurityContextHelper.getAuthenticatedOrganizationId();
        rateLimitService.checkRateLimit(organizationId);
        return reviewRequestService.listReviews(pullRequestId).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{reviewId}")
    public ReviewResponse getReview(@PathVariable UUID pullRequestId, @PathVariable UUID reviewId) {
        UUID organizationId = SecurityContextHelper.getAuthenticatedOrganizationId();
        rateLimitService.checkRateLimit(organizationId);
        ReviewEntity review = reviewRequestService.getReview(reviewId);
        if (!review.getPullRequestId().equals(pullRequestId)) {
            throw new com.deveshparmar.codesage.common.exception.InvalidRequestException(
                    "Review does not belong to pull request");
        }
        return toResponse(review);
    }

    @GetMapping("/{reviewId}/comments")
    public List<ReviewCommentResponse> listComments(@PathVariable UUID pullRequestId, @PathVariable UUID reviewId) {
        UUID organizationId = SecurityContextHelper.getAuthenticatedOrganizationId();
        rateLimitService.checkRateLimit(organizationId);
        ReviewEntity review = reviewRequestService.getReview(reviewId);
        if (!review.getPullRequestId().equals(pullRequestId)) {
            throw new com.deveshparmar.codesage.common.exception.InvalidRequestException(
                    "Review does not belong to pull request");
        }
        return reviewCommentQueryService.listCommentsByReviewId(reviewId).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    private ReviewCommentResponse toCommentResponse(ReviewCommentEntity entity) {
        return new ReviewCommentResponse(
                entity.getId(),
                entity.getReviewId(),
                entity.getFilePath(),
                entity.getStartLine(),
                entity.getEndLine(),
                entity.getSeverity(),
                entity.getCategory(),
                entity.getMessage(),
                entity.getSuggestion(),
                entity.getExternalCommentId(),
                entity.getCreatedAt()
        );
    }

    private ReviewResponse toResponse(ReviewEntity entity) {
        return new ReviewResponse(
                entity.getId(),
                entity.getPullRequestId(),
                entity.getStatus(),
                entity.getSummary(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt()
        );
    }
}
