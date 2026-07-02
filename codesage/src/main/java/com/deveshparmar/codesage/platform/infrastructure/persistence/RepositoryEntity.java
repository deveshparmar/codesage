package com.deveshparmar.codesage.platform.infrastructure.persistence;

import com.deveshparmar.codesage.common.domain.IndexingStatus;
import com.deveshparmar.codesage.common.domain.ScmProviderType;
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
@Table(name = "repositories")
@Getter
@Setter
public class RepositoryEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scm_provider", nullable = false, length = 32)
    private ScmProviderType scmProvider;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "clone_url", nullable = false)
    private String cloneUrl;

    @Column(name = "default_branch", nullable = false)
    private String defaultBranch;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate;

    @Enumerated(EnumType.STRING)
    @Column(name = "indexing_status", nullable = false, length = 32)
    private IndexingStatus indexingStatus = IndexingStatus.PENDING;

    @Column(name = "webhook_secret")
    private String webhookSecret;

    @Column(name = "scm_access_token")
    private String scmAccessToken;

    @Column(name = "installation_id", length = 64)
    private String installationId;

    @Column(name = "last_indexed_at")
    private Instant lastIndexedAt;

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
