package com.deveshparmar.codesage.indexing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IndexedFileJpaRepository extends JpaRepository<IndexedFileEntity, UUID> {

    Optional<IndexedFileEntity> findTopByBranchIdAndPathOrderByCreatedAtDesc(UUID branchId, String path);

    List<IndexedFileEntity> findByBranchId(UUID branchId);

    @Modifying
    @Query("DELETE FROM IndexedFileEntity f WHERE f.branchId = :branchId")
    void deleteByBranchId(UUID branchId);
}
