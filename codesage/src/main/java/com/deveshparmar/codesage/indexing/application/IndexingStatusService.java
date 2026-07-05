package com.deveshparmar.codesage.indexing.application;

import com.deveshparmar.codesage.common.domain.IndexingStatus;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IndexingStatusService {

    private final RepositoryJpaRepository repositoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markInProgress(UUID repositoryId) {
        updateStatus(repositoryId, IndexingStatus.IN_PROGRESS, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID repositoryId) {
        updateStatus(repositoryId, IndexingStatus.COMPLETED, Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID repositoryId) {
        updateStatus(repositoryId, IndexingStatus.FAILED, null);
    }

    private void updateStatus(UUID repositoryId, IndexingStatus status, Instant lastIndexedAt) {
        RepositoryEntity repository = repositoryRepository.findById(repositoryId).orElse(null);
        if (repository == null) {
            return;
        }
        repository.setIndexingStatus(status);
        if (lastIndexedAt != null) {
            repository.setLastIndexedAt(lastIndexedAt);
        }
        repositoryRepository.save(repository);
    }
}
