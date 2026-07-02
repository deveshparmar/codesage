package com.deveshparmar.codesage.indexing.application;

import com.deveshparmar.codesage.common.exception.InvalidRequestException;
import com.deveshparmar.codesage.platform.infrastructure.kafka.PlatformEventPublisher;
import com.deveshparmar.codesage.platform.infrastructure.kafka.RepositoryIndexRequestedPayload;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IndexingTriggerService {

    private final RepositoryJpaRepository repositoryRepository;
    private final PlatformEventPublisher platformEventPublisher;

    public void triggerIndexing(UUID organizationId, UUID repositoryId, String branchName, String commitSha, boolean fullReindex) {
        RepositoryEntity repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new InvalidRequestException("Repository not found: " + repositoryId));
        if (!repository.getOrganizationId().equals(organizationId)) {
            throw new InvalidRequestException("Repository does not belong to organization");
        }

        platformEventPublisher.publishRepositoryIndexRequested(
                organizationId,
                UUID.randomUUID(),
                new RepositoryIndexRequestedPayload(
                        repositoryId,
                        branchName != null ? branchName : repository.getDefaultBranch(),
                        commitSha,
                        fullReindex
                )
        );
    }
}
