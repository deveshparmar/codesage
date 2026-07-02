package com.deveshparmar.codesage.rag.api;

import com.deveshparmar.codesage.platform.api.SecurityContextHelper;
import com.deveshparmar.codesage.platform.application.RepositoryService;
import com.deveshparmar.codesage.platform.infrastructure.redis.RateLimitService;
import com.deveshparmar.codesage.rag.api.dto.SemanticSearchRequest;
import com.deveshparmar.codesage.rag.api.dto.SemanticSearchResultResponse;
import com.deveshparmar.codesage.rag.application.SemanticSearchService;
import com.deveshparmar.codesage.rag.domain.RetrievedContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;
    private final RepositoryService repositoryService;
    private final RateLimitService rateLimitService;

    @PostMapping("/semantic")
    public List<SemanticSearchResultResponse> search(@Valid @RequestBody SemanticSearchRequest request) {
        UUID organizationId = SecurityContextHelper.getAuthenticatedOrganizationId();
        rateLimitService.checkRateLimit(organizationId);

        var repository = repositoryService.getById(request.repositoryId());
        if (!repository.getOrganizationId().equals(organizationId)) {
            throw new com.deveshparmar.codesage.common.exception.InvalidRequestException(
                    "Repository does not belong to organization");
        }

        return semanticSearchService.search(
                request.repositoryId(),
                request.query(),
                request.topK(),
                request.minSimilarity(),
                request.chunkType(),
                request.packageName(),
                request.className(),
                request.filePath()
        ).stream().map(this::toResponse).toList();
    }

    private SemanticSearchResultResponse toResponse(RetrievedContext context) {
        return new SemanticSearchResultResponse(
                context.chunkId(),
                context.chunkType(),
                context.packageName(),
                context.className(),
                context.methodName(),
                context.filePath(),
                context.startLine(),
                context.endLine(),
                context.content(),
                context.similarity(),
                context.metadata()
        );
    }
}
