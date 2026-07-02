package com.deveshparmar.codesage.platform.application;

import com.deveshparmar.codesage.common.domain.PullRequestState;
import com.deveshparmar.codesage.common.domain.ScmProviderType;
import com.deveshparmar.codesage.common.exception.InvalidRequestException;
import com.deveshparmar.codesage.platform.infrastructure.kafka.RepositoryIndexRequestedPayload;
import com.deveshparmar.codesage.platform.infrastructure.kafka.PlatformEventPublisher;
import com.deveshparmar.codesage.platform.infrastructure.persistence.PullRequestEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.PullRequestJpaRepository;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryEntity;
import com.deveshparmar.codesage.platform.infrastructure.redis.WebhookDeduplicationService;
import com.deveshparmar.codesage.scm.application.ScmProviderRegistry;
import com.deveshparmar.codesage.scm.domain.WebhookEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final ScmProviderRegistry scmProviderRegistry;
    private final RepositoryService repositoryService;
    private final PullRequestJpaRepository pullRequestRepository;
    private final ReviewRequestService reviewRequestService;
    private final WebhookDeduplicationService deduplicationService;
    private final PlatformEventPublisher eventPublisher;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handleGitHubWebhook(
            UUID organizationId,
            String eventType,
            String deliveryId,
            String signature,
            String payload,
            String ipAddress
    ) {
        if (deduplicationService.isDuplicate(deliveryId)) {
            log.info("Duplicate webhook delivery ignored: {}", deliveryId);
            return;
        }

        WebhookEvent event = scmProviderRegistry.getProvider(ScmProviderType.GITHUB)
                .getWebhookVerifier()
                .parseEvent(eventType, deliveryId, payload);

        JsonNode root = parsePayload(payload);
        String repositoryFullName = extractRepositoryFullName(root);
        RepositoryEntity repository = repositoryService.findByProviderAndFullName(ScmProviderType.GITHUB, repositoryFullName);

        if (!repository.getOrganizationId().equals(organizationId)) {
            throw new InvalidRequestException("Repository does not belong to organization");
        }

        scmProviderRegistry.getProvider(ScmProviderType.GITHUB)
                .getWebhookVerifier()
                .verify(payload, signature, repository.getWebhookSecret());

        UUID correlationId = UUID.randomUUID();
        auditService.record(
                organizationId,
                correlationId,
                "WEBHOOK_RECEIVED",
                "github-webhook",
                eventType.toUpperCase(),
                deliveryId,
                Map.of("action", event.action() != null ? event.action() : "", "repository", repositoryFullName),
                ipAddress
        );

        if ("pull_request".equalsIgnoreCase(eventType)) {
            handlePullRequestEvent(repository, event, root, correlationId);
        } else if ("push".equalsIgnoreCase(eventType)) {
            handlePushEvent(repository, root, correlationId);
        }
    }

    private void handlePullRequestEvent(RepositoryEntity repository, WebhookEvent event, JsonNode root, UUID correlationId) {
        String action = event.action();
        if (action == null || (!action.equals("opened") && !action.equals("synchronize") && !action.equals("reopened"))) {
            log.info("Ignoring pull_request action: {}", action);
            return;
        }

        JsonNode prNode = root.path("pull_request");
        PullRequestEntity pullRequest = upsertPullRequest(repository.getId(), prNode);
        reviewRequestService.requestReview(repository, pullRequest, "WEBHOOK", correlationId);
    }

    private void handlePushEvent(RepositoryEntity repository, JsonNode root, UUID correlationId) {
        String ref = root.path("ref").asText();
        if (!ref.startsWith("refs/heads/")) {
            return;
        }
        String branchName = ref.substring("refs/heads/".length());
        if (!branchName.equals(repository.getDefaultBranch())) {
            return;
        }
        String commitSha = root.path("after").asText();
        if (commitSha == null || commitSha.isBlank() || "0000000000000000000000000000000000000000".equals(commitSha)) {
            return;
        }

        eventPublisher.publishRepositoryIndexRequested(
                repository.getOrganizationId(),
                correlationId,
                new RepositoryIndexRequestedPayload(repository.getId(), branchName, commitSha, false)
        );
    }

    private PullRequestEntity upsertPullRequest(UUID repositoryId, JsonNode prNode) {
        String externalId = prNode.path("id").asText();
        PullRequestEntity entity = pullRequestRepository.findByRepositoryIdAndExternalId(repositoryId, externalId)
                .orElseGet(PullRequestEntity::new);

        entity.setRepositoryId(repositoryId);
        entity.setExternalId(externalId);
        entity.setNumber(prNode.path("number").asInt());
        entity.setTitle(prNode.path("title").asText());
        entity.setSourceBranch(stripRefPrefix(prNode.path("head").path("ref").asText()));
        entity.setTargetBranch(stripRefPrefix(prNode.path("base").path("ref").asText()));
        entity.setHeadSha(prNode.path("head").path("sha").asText());
        entity.setBaseSha(prNode.path("base").path("sha").asText());
        entity.setState(mapPullRequestState(prNode.path("state").asText()));
        entity.setAuthor(prNode.path("user").path("login").asText("unknown"));
        return pullRequestRepository.save(entity);
    }

    private PullRequestState mapPullRequestState(String state) {
        return switch (state.toLowerCase()) {
            case "open" -> PullRequestState.OPEN;
            case "closed" -> PullRequestState.CLOSED;
            default -> PullRequestState.CLOSED;
        };
    }

    private String stripRefPrefix(String ref) {
        if (ref.startsWith("refs/heads/")) {
            return ref.substring("refs/heads/".length());
        }
        return ref;
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new InvalidRequestException("Invalid webhook payload");
        }
    }

    private String extractRepositoryFullName(JsonNode root) {
        String fullName = root.path("repository").path("full_name").asText(null);
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidRequestException("Webhook payload missing repository.full_name");
        }
        return fullName;
    }
}
