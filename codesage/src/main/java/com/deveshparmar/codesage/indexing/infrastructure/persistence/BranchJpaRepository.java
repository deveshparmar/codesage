package com.deveshparmar.codesage.indexing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BranchJpaRepository extends JpaRepository<BranchEntity, UUID> {

    Optional<BranchEntity> findByRepositoryIdAndName(UUID repositoryId, String name);
}
