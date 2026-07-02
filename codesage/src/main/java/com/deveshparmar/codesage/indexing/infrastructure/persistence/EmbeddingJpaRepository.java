package com.deveshparmar.codesage.indexing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmbeddingJpaRepository extends JpaRepository<EmbeddingEntity, UUID> {

    Optional<EmbeddingEntity> findByChunkId(UUID chunkId);

    boolean existsByChunkId(UUID chunkId);

    @Modifying
    @Query("DELETE FROM EmbeddingEntity e WHERE e.chunkId IN :chunkIds")
    void deleteByChunkIdIn(List<UUID> chunkIds);
}
