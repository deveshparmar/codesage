package com.deveshparmar.codesage.indexing.api;

import com.deveshparmar.codesage.indexing.api.dto.IndexStatusResponse;
import com.deveshparmar.codesage.indexing.api.dto.TriggerIndexRequest;
import com.deveshparmar.codesage.indexing.application.IndexingTriggerService;
import com.deveshparmar.codesage.platform.api.SecurityContextHelper;
import com.deveshparmar.codesage.platform.application.RepositoryService;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryEntity;
import com.deveshparmar.codesage.platform.infrastructure.redis.RateLimitService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repositories")
@RequiredArgsConstructor
public class IndexingController {

    private final IndexingTriggerService indexingTriggerService;
    private final RepositoryService repositoryService;
    private final RateLimitService rateLimitService;

    @PostMapping("/index")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IndexStatusResponse triggerIndex(@Valid @RequestBody TriggerIndexRequest request) {
        UUID organizationId = SecurityContextHelper.getAuthenticatedOrganizationId();
        rateLimitService.checkRateLimit(organizationId);

        indexingTriggerService.triggerIndexing(
                organizationId,
                request.repositoryId(),
                request.branchName(),
                request.commitSha(),
                request.fullReindex()
        );

        RepositoryEntity repository = repositoryService.getById(request.repositoryId());
        return new IndexStatusResponse(
                repository.getId(),
                repository.getIndexingStatus(),
                repository.getLastIndexedAt(),
                "Indexing request accepted"
        );
    }

    @GetMapping("/{repositoryId}/index-status")
    public IndexStatusResponse getIndexStatus(@PathVariable UUID repositoryId) {
        UUID organizationId = SecurityContextHelper.getAuthenticatedOrganizationId();
        rateLimitService.checkRateLimit(organizationId);

        RepositoryEntity repository = repositoryService.getById(repositoryId);
        if (!repository.getOrganizationId().equals(organizationId)) {
            throw new com.deveshparmar.codesage.common.exception.InvalidRequestException(
                    "Repository does not belong to organization");
        }

        return new IndexStatusResponse(
                repository.getId(),
                repository.getIndexingStatus(),
                repository.getLastIndexedAt(),
                "Current indexing status"
        );
    }
}
