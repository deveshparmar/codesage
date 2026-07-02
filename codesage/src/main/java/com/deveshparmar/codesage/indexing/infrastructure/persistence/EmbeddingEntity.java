package com.deveshparmar.codesage.indexing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "embeddings")
@Getter
@Setter
public class EmbeddingEntity {

    @Id
    private UUID id;

    @Column(name = "chunk_id", nullable = false, unique = true)
    private UUID chunkId;

    @Column(nullable = false, length = 64)
    private String model;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }
}
