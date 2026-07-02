package com.deveshparmar.codesage.platform.api;

import com.deveshparmar.codesage.indexing.application.IndexingTriggerService;
import com.deveshparmar.codesage.platform.api.dto.RegisterRepositoryRequest;
import com.deveshparmar.codesage.platform.api.dto.RepositoryResponse;
import com.deveshparmar.codesage.platform.application.RepositoryService;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryEntity;
import com.deveshparmar.codesage.platform.infrastructure.redis.RateLimitService;
import com.deveshparmar.codesage.scm.domain.ScmAccessToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final RateLimitService rateLimitService;
    private final IndexingTriggerService indexingTriggerService;

    @GetMapping
    public List<RepositoryResponse> list(@PathVariable UUID organizationId) {
        UUID authenticatedOrgId = SecurityContextHelper.getAuthenticatedOrganizationId();
        rateLimitService.checkRateLimit(authenticatedOrgId);
        if (!authenticatedOrgId.equals(organizationId)) {
            throw new com.deveshparmar.codesage.common.exception.InvalidRequestException(
                    "Cannot access repositories for another organization");
        }
        return repositoryService.listByOrganization(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RepositoryResponse register(
            @PathVariable UUID organizationId,
            @Valid @RequestBody RegisterRepositoryRequest request
    ) {
        UUID authenticatedOrgId = SecurityContextHelper.getAuthenticatedOrganizationId();
        rateLimitService.checkRateLimit(authenticatedOrgId);
        if (!authenticatedOrgId.equals(organizationId)) {
            throw new com.deveshparmar.codesage.common.exception.InvalidRequestException(
                    "Cannot register repositories for another organization");
        }
        RepositoryEntity entity = repositoryService.registerRepository(
                organizationId,
                request.provider(),
                request.owner(),
                request.repositoryName(),
                new ScmAccessToken(request.accessToken()),
                request.webhookSecret()
        );
        indexingTriggerService.triggerIndexing(
                organizationId,
                entity.getId(),
                entity.getDefaultBranch(),
                null,
                true
        );
        return toResponse(entity);
    }

    private RepositoryResponse toResponse(RepositoryEntity entity) {
        return new RepositoryResponse(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getScmProvider(),
                entity.getExternalId(),
                entity.getName(),
                entity.getFullName(),
                entity.getDefaultBranch(),
                entity.isPrivate(),
                entity.getIndexingStatus(),
                entity.getLastIndexedAt(),
                entity.getCreatedAt()
        );
    }
}
