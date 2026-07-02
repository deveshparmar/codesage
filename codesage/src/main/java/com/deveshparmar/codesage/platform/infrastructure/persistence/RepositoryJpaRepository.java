package com.deveshparmar.codesage.platform.infrastructure.persistence;

import com.deveshparmar.codesage.common.domain.ScmProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryJpaRepository extends JpaRepository<RepositoryEntity, UUID> {

    List<RepositoryEntity> findByOrganizationId(UUID organizationId);

    Optional<RepositoryEntity> findByOrganizationIdAndScmProviderAndExternalId(
            UUID organizationId,
            ScmProviderType scmProvider,
            String externalId
    );

    Optional<RepositoryEntity> findByScmProviderAndFullName(ScmProviderType scmProvider, String fullName);
}
