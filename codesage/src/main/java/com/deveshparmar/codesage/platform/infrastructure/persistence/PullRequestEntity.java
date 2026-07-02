package com.deveshparmar.codesage.platform.infrastructure.persistence;

import com.deveshparmar.codesage.common.domain.PullRequestState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pull_requests")
@Getter
@Setter
public class PullRequestEntity {

    @Id
    private UUID id;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String title;

    @Column(name = "source_branch", nullable = false)
    private String sourceBranch;

    @Column(name = "target_branch", nullable = false)
    private String targetBranch;

    @Column(name = "head_sha", nullable = false, length = 40)
    private String headSha;

    @Column(name = "base_sha", nullable = false, length = 40)
    private String baseSha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PullRequestState state;

    @Column(nullable = false)
    private String author;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
