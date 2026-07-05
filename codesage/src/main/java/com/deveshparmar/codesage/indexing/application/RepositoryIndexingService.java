package com.deveshparmar.codesage.indexing.application;

import com.deveshparmar.codesage.common.exception.CodeSageException;
import com.deveshparmar.codesage.common.exception.InvalidRequestException;
import com.deveshparmar.codesage.indexing.domain.ClonedRepository;
import com.deveshparmar.codesage.indexing.domain.IndexingResult;
import com.deveshparmar.codesage.indexing.domain.ParsedSourceFile;
import com.deveshparmar.codesage.indexing.domain.RepositoryClonePort;
import com.deveshparmar.codesage.indexing.infrastructure.kafka.IndexingEventPublisher;
import com.deveshparmar.codesage.indexing.infrastructure.persistence.BranchEntity;
import com.deveshparmar.codesage.indexing.infrastructure.redis.IndexingLockService;
import com.deveshparmar.codesage.llm.config.OpenAiProperties;
import com.deveshparmar.codesage.platform.infrastructure.kafka.RepositoryIndexCompletedPayload;
import com.deveshparmar.codesage.platform.infrastructure.kafka.RepositoryIndexRequestedPayload;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryIndexingService {

    private final RepositoryJpaRepository repositoryRepository;
    private final RepositoryClonePort repositoryClonePort;
    private final SourceFileDiscoveryService sourceFileDiscoveryService;
    private final ChunkPersistenceService chunkPersistenceService;
    private final IndexingLockService indexingLockService;
    private final IndexingStatusService indexingStatusService;
    private final IndexingEventPublisher indexingEventPublisher;
    private final OpenAiProperties openAiProperties;

    @Transactional
    public IndexingResult indexRepository(UUID organizationId, UUID correlationId, RepositoryIndexRequestedPayload request) {
        if (!indexingLockService.tryAcquire(request.repositoryId())) {
            throw new InvalidRequestException("Indexing already in progress for repository " + request.repositoryId());
        }

        long startedAt = System.currentTimeMillis();
        RepositoryEntity repository = repositoryRepository.findById(request.repositoryId())
                .orElseThrow(() -> new CodeSageException("Repository not found: " + request.repositoryId()));

        if (!repository.getOrganizationId().equals(organizationId)) {
            indexingLockService.release(request.repositoryId());
            throw new InvalidRequestException("Repository does not belong to organization");
        }
        if (repository.getScmAccessToken() == null || repository.getScmAccessToken().isBlank()) {
            indexingLockService.release(request.repositoryId());
            throw new InvalidRequestException("Repository SCM access token is not configured");
        }

        indexingStatusService.markInProgress(request.repositoryId());

        String branchName = request.branchName() != null && !request.branchName().isBlank()
                ? request.branchName()
                : repository.getDefaultBranch();
        String commitSha = request.commitSha();

        try {
            if (request.fullReindex()) {
                chunkPersistenceService.clearBranchIndexIfExists(repository.getId(), branchName);
            }

            ClonedRepository clonedRepository = repositoryClonePort.cloneOrUpdate(
                    repository.getId(),
                    repository.getCloneUrl(),
                    repository.getScmAccessToken(),
                    branchName,
                    commitSha,
                    request.fullReindex()
            );

            if (commitSha == null || commitSha.isBlank()) {
                commitSha = clonedRepository.commitSha();
            }

            BranchEntity branch = chunkPersistenceService.upsertBranch(
                    repository.getId(),
                    branchName,
                    commitSha,
                    branchName.equals(repository.getDefaultBranch())
            );

            List<ParsedSourceFile> parsedFiles = sourceFileDiscoveryService.discoverAndParse(clonedRepository);
            List<UUID> chunkIdsForEmbedding = new ArrayList<>();
            int filesIndexed = 0;

            for (ParsedSourceFile parsedFile : parsedFiles) {
                ChunkPersistenceService.PersistedFileResult result = chunkPersistenceService.persistParsedFile(
                        branch,
                        parsedFile,
                        commitSha,
                        request.fullReindex()
                );
                if (result.changed()) {
                    filesIndexed++;
                }
                chunkIdsForEmbedding.addAll(result.newChunkIds());
            }

            chunkPersistenceService.markBranchIndexed(branch.getId());

            indexingStatusService.markCompleted(request.repositoryId());

            IndexingResult indexingResult = new IndexingResult(
                    repository.getId(),
                    branchName,
                    commitSha,
                    filesIndexed,
                    chunkIdsForEmbedding.size(),
                    chunkIdsForEmbedding.size(),
                    System.currentTimeMillis() - startedAt,
                    IndexingResult.IndexingStatus.SUCCESS,
                    chunkIdsForEmbedding
            );

            if (!chunkIdsForEmbedding.isEmpty()) {
                indexingEventPublisher.publishEmbeddingGenerationRequested(
                        organizationId,
                        correlationId,
                        repository.getId(),
                        chunkIdsForEmbedding,
                        openAiProperties.getEmbeddingModel()
                );
            }

            publishCompleted(organizationId, correlationId, indexingResult);
            return indexingResult;
        } catch (Exception ex) {
            indexingStatusService.markFailed(request.repositoryId());
            throw new CodeSageException("Repository indexing failed for " + repository.getId(), ex);
        } finally {
            indexingLockService.release(request.repositoryId());
        }
    }

    public void publishCompleted(UUID organizationId, UUID correlationId, IndexingResult result) {
        indexingEventPublisher.publishRepositoryIndexCompleted(
                organizationId,
                correlationId,
                new RepositoryIndexCompletedPayload(
                        result.repositoryId(),
                        result.branchName(),
                        result.commitSha(),
                        result.filesIndexed(),
                        result.chunksCreated(),
                        result.embeddingsQueued(),
                        result.durationMs(),
                        result.status().name()
                )
        );
    }
}
