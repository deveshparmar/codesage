package com.deveshparmar.codesage.review.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewCommentJpaRepository extends JpaRepository<ReviewCommentEntity, UUID> {

    List<ReviewCommentEntity> findByReviewIdOrderByStartLineAsc(UUID reviewId);
}
