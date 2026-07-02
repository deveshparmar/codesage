package com.deveshparmar.codesage.platform.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, UUID> {

    List<ReviewEntity> findByPullRequestIdOrderByCreatedAtDesc(UUID pullRequestId);
}
