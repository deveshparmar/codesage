package com.deveshparmar.codesage.platform.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PullRequestJpaRepository extends JpaRepository<PullRequestEntity, UUID> {

    Optional<PullRequestEntity> findByRepositoryIdAndExternalId(UUID repositoryId, String externalId);

    Optional<PullRequestEntity> findByRepositoryIdAndNumber(UUID repositoryId, int number);
}
