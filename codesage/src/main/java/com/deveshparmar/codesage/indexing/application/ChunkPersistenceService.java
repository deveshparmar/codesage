package com.deveshparmar.codesage.indexing.application;

import com.deveshparmar.codesage.indexing.domain.CodeChunk;
import com.deveshparmar.codesage.indexing.domain.ParsedSourceFile;
import com.deveshparmar.codesage.indexing.infrastructure.persistence.BranchEntity;
import com.deveshparmar.codesage.indexing.infrastructure.persistence.BranchJpaRepository;
import com.deveshparmar.codesage.indexing.infrastructure.persistence.ChunkEntity;
import com.deveshparmar.codesage.indexing.infrastructure.persistence.ChunkJpaRepository;
import com.deveshparmar.codesage.indexing.infrastructure.persistence.EmbeddingJpaRepository;
import com.deveshparmar.codesage.indexing.infrastructure.persistence.IndexedFileEntity;
import com.deveshparmar.codesage.indexing.infrastructure.persistence.IndexedFileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChunkPersistenceService {

    private final BranchJpaRepository branchRepository;
    private final IndexedFileJpaRepository fileRepository;
    private final ChunkJpaRepository chunkRepository;
    private final EmbeddingJpaRepository embeddingRepository;

    @Transactional
    public BranchEntity upsertBranch(UUID repositoryId, String branchName, String commitSha, boolean isDefault) {
        BranchEntity branch = branchRepository.findByRepositoryIdAndName(repositoryId, branchName)
                .orElseGet(BranchEntity::new);
        branch.setRepositoryId(repositoryId);
        branch.setName(branchName);
        branch.setHeadCommitSha(commitSha);
        branch.setDefault(isDefault);
        return branchRepository.save(branch);
    }

    @Transactional
    public PersistedFileResult persistParsedFile(
            BranchEntity branch,
            ParsedSourceFile parsedFile,
            String commitSha,
            boolean fullReindex
    ) {
        Optional<IndexedFileEntity> existingOpt = fileRepository
                .findTopByBranchIdAndPathOrderByCreatedAtDesc(branch.getId(), parsedFile.relativePath().toString());

        if (!fullReindex && existingOpt.isPresent()) {
            IndexedFileEntity existing = existingOpt.get();
            if (existing.getContentHash().equals(parsedFile.contentHash())
                    && existing.getLastCommitSha().equals(commitSha)) {
                return new PersistedFileResult(existing.getId(), List.of(), false);
            }
            removeFileChunks(existing.getId());
        }

        IndexedFileEntity fileEntity = existingOpt.orElseGet(IndexedFileEntity::new);
        fileEntity.setBranchId(branch.getId());
        fileEntity.setPath(parsedFile.relativePath().toString());
        fileEntity.setLanguage(parsedFile.language());
        fileEntity.setContentHash(parsedFile.contentHash());
        fileEntity.setLastCommitSha(commitSha);
        fileEntity = fileRepository.save(fileEntity);

        List<UUID> chunkIds = new ArrayList<>();
        for (CodeChunk chunk : parsedFile.chunks()) {
            ChunkEntity chunkEntity = chunkRepository.findByFileIdAndChunkHash(fileEntity.getId(), chunk.chunkHash())
                    .orElseGet(ChunkEntity::new);
            chunkEntity.setFileId(fileEntity.getId());
            chunkEntity.setChunkType(chunk.chunkType());
            chunkEntity.setChunkHash(chunk.chunkHash());
            chunkEntity.setPackageName(chunk.packageName());
            chunkEntity.setClassName(chunk.className());
            chunkEntity.setMethodName(chunk.methodName());
            chunkEntity.setStartLine(chunk.startLine());
            chunkEntity.setEndLine(chunk.endLine());
            chunkEntity.setContent(chunk.content());
            chunkEntity.setMetadata(chunk.metadata());
            chunkEntity = chunkRepository.save(chunkEntity);

            if (!embeddingRepository.existsByChunkId(chunkEntity.getId())) {
                chunkIds.add(chunkEntity.getId());
            }
        }

        return new PersistedFileResult(fileEntity.getId(), chunkIds, true);
    }

    @Transactional
    public void markBranchIndexed(UUID branchId) {
        branchRepository.findById(branchId).ifPresent(branch -> {
            branch.setLastIndexedAt(Instant.now());
            branchRepository.save(branch);
        });
    }

    @Transactional
    public void clearBranchIndex(UUID branchId) {
        List<IndexedFileEntity> files = fileRepository.findByBranchId(branchId);
        if (!files.isEmpty()) {
            for (IndexedFileEntity file : files) {
                removeFileChunks(file.getId());
            }
            fileRepository.deleteByBranchId(branchId);
        }
    }

    private void removeFileChunks(UUID fileId) {
        List<ChunkEntity> chunks = chunkRepository.findByFileId(fileId);
        if (!chunks.isEmpty()) {
            List<UUID> chunkIds = chunks.stream().map(ChunkEntity::getId).toList();
            embeddingRepository.deleteByChunkIdIn(chunkIds);
            chunkRepository.deleteAll(chunks);
        }
    }

    public record PersistedFileResult(UUID fileId, List<UUID> newChunkIds, boolean changed) {
    }
}
