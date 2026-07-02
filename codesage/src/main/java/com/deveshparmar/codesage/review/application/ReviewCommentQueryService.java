package com.deveshparmar.codesage.review.application;

import com.deveshparmar.codesage.common.exception.ResourceNotFoundException;
import com.deveshparmar.codesage.review.infrastructure.persistence.ReviewCommentEntity;
import com.deveshparmar.codesage.review.infrastructure.persistence.ReviewCommentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewCommentQueryService {

    private final ReviewCommentJpaRepository reviewCommentRepository;

    @Transactional(readOnly = true)
    public List<ReviewCommentEntity> getCommentsByReviewId(UUID reviewId) {
        List<ReviewCommentEntity> comments = reviewCommentRepository.findByReviewIdOrderByStartLineAsc(reviewId);
        if (comments.isEmpty()) {
            throw new ResourceNotFoundException("Review comments", reviewId.toString());
        }
        return comments;
    }

    @Transactional(readOnly = true)
    public List<ReviewCommentEntity> listCommentsByReviewId(UUID reviewId) {
        return reviewCommentRepository.findByReviewIdOrderByStartLineAsc(reviewId);
    }
}
