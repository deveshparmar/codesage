package com.deveshparmar.codesage.platform.application;

import com.deveshparmar.codesage.common.domain.ScmProviderType;
import com.deveshparmar.codesage.common.exception.DuplicateResourceException;
import com.deveshparmar.codesage.common.exception.ResourceNotFoundException;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryJpaRepository;
import com.deveshparmar.codesage.scm.application.ScmProviderRegistry;
import com.deveshparmar.codesage.scm.domain.ScmAccessToken;
import com.deveshparmar.codesage.scm.domain.ScmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final RepositoryJpaRepository repositoryRepository;
    private final OrganizationService organizationService;
    private final ScmProviderRegistry scmProviderRegistry;

    @Transactional(readOnly = true)
    public RepositoryEntity getById(UUID repositoryId) {
        return repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId.toString()));
    }

    @Transactional(readOnly = true)
    public List<RepositoryEntity> listByOrganization(UUID organizationId) {
        organizationService.getById(organizationId);
        return repositoryRepository.findByOrganizationId(organizationId);
    }

    @Transactional
    public RepositoryEntity registerRepository(
            UUID organizationId,
            ScmProviderType providerType,
            String owner,
            String repositoryName,
            ScmAccessToken accessToken,
            String webhookSecret
    ) {
        organizationService.getById(organizationId);
        ScmRepository scmRepository = scmProviderRegistry.getProvider(providerType)
                .getRepositoryProvider()
                .fetchRepository(accessToken, owner, repositoryName);

        repositoryRepository.findByOrganizationIdAndScmProviderAndExternalId(
                organizationId,
                providerType,
                scmRepository.externalId()
        ).ifPresent(existing -> {
            throw new DuplicateResourceException("Repository already registered: " + scmRepository.fullName());
        });

        RepositoryEntity entity = new RepositoryEntity();
        entity.setOrganizationId(organizationId);
        entity.setScmProvider(providerType);
        entity.setExternalId(scmRepository.externalId());
        entity.setName(scmRepository.name());
        entity.setFullName(scmRepository.fullName());
        entity.setCloneUrl(scmRepository.cloneUrl());
        entity.setDefaultBranch(scmRepository.defaultBranch());
        entity.setPrivate(scmRepository.isPrivate());
        entity.setWebhookSecret(webhookSecret);
        entity.setScmAccessToken(accessToken.value());
        return repositoryRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public RepositoryEntity findByProviderAndFullName(ScmProviderType provider, String fullName) {
        return repositoryRepository.findByScmProviderAndFullName(provider, fullName)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", fullName));
    }
}
