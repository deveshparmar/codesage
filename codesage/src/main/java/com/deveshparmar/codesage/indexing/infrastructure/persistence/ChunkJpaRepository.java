package com.deveshparmar.codesage.indexing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChunkJpaRepository extends JpaRepository<ChunkEntity, UUID> {

    Optional<ChunkEntity> findByFileIdAndChunkHash(UUID fileId, String chunkHash);

    List<ChunkEntity> findByFileId(UUID fileId);

    List<ChunkEntity> findByFileIdIn(List<UUID> fileIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChunkEntity c WHERE c.fileId IN :fileIds")
    void deleteByFileIdIn(List<UUID> fileIds);

    @Query("""
            SELECT c FROM ChunkEntity c
            JOIN IndexedFileEntity f ON f.id = c.fileId
            JOIN BranchEntity b ON b.id = f.branchId
            WHERE b.repositoryId = :repositoryId AND c.id IN :chunkIds
            """)
    List<ChunkEntity> findByRepositoryIdAndChunkIds(UUID repositoryId, List<UUID> chunkIds);
}
