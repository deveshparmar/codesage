package com.deveshparmar.codesage.review.application;

import com.deveshparmar.codesage.common.domain.ReviewStatus;
import com.deveshparmar.codesage.common.domain.Severity;
import com.deveshparmar.codesage.common.exception.CodeSageException;
import com.deveshparmar.codesage.llm.application.CodeReviewLlmService;
import com.deveshparmar.codesage.llm.config.OpenAiProperties;
import com.deveshparmar.codesage.platform.application.AuditService;
import com.deveshparmar.codesage.platform.infrastructure.kafka.ReviewCompletedPayload;
import com.deveshparmar.codesage.platform.infrastructure.kafka.ReviewRequestedPayload;
import com.deveshparmar.codesage.platform.infrastructure.persistence.PullRequestEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.PullRequestJpaRepository;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.RepositoryJpaRepository;
import com.deveshparmar.codesage.platform.infrastructure.persistence.ReviewEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.ReviewJpaRepository;
import com.deveshparmar.codesage.rag.application.ContextRetrievalService;
import com.deveshparmar.codesage.rag.domain.RetrievedContext;
import com.deveshparmar.codesage.review.config.ReviewProperties;
import com.deveshparmar.codesage.review.domain.LlmReviewResponse;
import com.deveshparmar.codesage.review.domain.ModifiedMethod;
import com.deveshparmar.codesage.review.domain.ReviewFinding;
import com.deveshparmar.codesage.review.infrastructure.kafka.ReviewEventPublisher;
import com.deveshparmar.codesage.review.infrastructure.persistence.ReviewCommentEntity;
import com.deveshparmar.codesage.review.infrastructure.persistence.ReviewCommentJpaRepository;
import com.deveshparmar.codesage.scm.application.ScmProviderRegistry;
import com.deveshparmar.codesage.scm.domain.PullRequestProvider;
import com.deveshparmar.codesage.scm.domain.ReviewCommentRequest;
import com.deveshparmar.codesage.scm.domain.ReviewPublisher;
import com.deveshparmar.codesage.scm.domain.ReviewSubmission;
import com.deveshparmar.codesage.scm.domain.ScmAccessToken;
import com.deveshparmar.codesage.scm.domain.ScmPullRequestFile;
import com.deveshparmar.codesage.scm.domain.ScmReview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PullRequestReviewService {

    private final ReviewJpaRepository reviewRepository;
    private final PullRequestJpaRepository pullRequestRepository;
    private final RepositoryJpaRepository repositoryRepository;
    private final ReviewCommentJpaRepository reviewCommentRepository;
    private final ScmProviderRegistry scmProviderRegistry;
    private final ModifiedMethodLocator modifiedMethodLocator;
    private final ReviewPromptBuilder reviewPromptBuilder;
    private final ContextRetrievalService contextRetrievalService;
    private final CodeReviewLlmService codeReviewLlmService;
    private final FindingRanker findingRanker;
    private final FindingDeduplicator findingDeduplicator;
    private final ReviewEventPublisher reviewEventPublisher;
    private final AuditService auditService;
    private final ReviewProperties reviewProperties;
    private final OpenAiProperties openAiProperties;

    @Transactional
    public void executeReview(UUID organizationId, UUID correlationId, ReviewRequestedPayload payload) {
        long startedAt = System.currentTimeMillis();
        ReviewEntity review = reviewRepository.findById(payload.reviewId())
                .orElseThrow(() -> new CodeSageException("Review not found: " + payload.reviewId()));

        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.setStartedAt(Instant.now());
        review.setLlmModel(openAiProperties.getChatModel());
        reviewRepository.save(review);

        try {
            PullRequestEntity pullRequest = pullRequestRepository.findById(payload.pullRequestId())
                    .orElseThrow(() -> new CodeSageException("Pull request not found: " + payload.pullRequestId()));
            RepositoryEntity repository = repositoryRepository.findById(payload.repositoryId())
                    .orElseThrow(() -> new CodeSageException("Repository not found: " + payload.repositoryId()));

            if (!repository.getOrganizationId().equals(organizationId)) {
                throw new CodeSageException("Repository does not belong to organization");
            }

            ScmAccessToken accessToken = new ScmAccessToken(repository.getScmAccessToken());
            String[] coordinates = parseRepositoryCoordinates(repository.getFullName());
            String owner = coordinates[0];
            String repoName = coordinates[1];

            PullRequestProvider pullRequestProvider = scmProviderRegistry.getProvider(repository.getScmProvider())
                    .getPullRequestProvider();
            ReviewPublisher reviewPublisher = scmProviderRegistry.getProvider(repository.getScmProvider())
                    .getReviewPublisher();

            List<ScmPullRequestFile> changedFiles = pullRequestProvider.fetchChangedFiles(
                    accessToken, owner, repoName, payload.pullRequestNumber()
            );

            List<ModifiedMethod> modifiedMethods = modifiedMethodLocator.locateModifiedMethods(
                    repository.getId(),
                    changedFiles
            ).stream()
                    .limit(reviewProperties.getMaxMethodsPerReview())
                    .toList();

            List<ReviewFinding> allFindings = new ArrayList<>();
            int totalPromptTokens = 0;
            int totalCompletionTokens = 0;
            StringBuilder summaryBuilder = new StringBuilder();

            for (ModifiedMethod method : modifiedMethods) {
                LlmReviewResponse llmResponse = reviewMethod(pullRequest.getTitle(), repository.getId(), method);
                totalPromptTokens += llmResponse.promptTokens();
                totalCompletionTokens += llmResponse.completionTokens();
                if (llmResponse.summary() != null && !llmResponse.summary().isBlank()) {
                    summaryBuilder.append("- ").append(method.signature()).append(": ")
                            .append(llmResponse.summary()).append("\n");
                }
                allFindings.addAll(mapFindings(method, llmResponse));
            }

            List<ReviewFinding> rankedFindings = findingRanker.rank(allFindings);
            List<ReviewFinding> deduplicatedFindings = findingDeduplicator.deduplicate(rankedFindings);

            List<ReviewCommentEntity> persistedComments = persistFindings(review.getId(), deduplicatedFindings);
            int commentsPosted = publishToScm(
                    reviewPublisher,
                    accessToken,
                    owner,
                    repoName,
                    payload.pullRequestNumber(),
                    summaryBuilder.toString().trim(),
                    deduplicatedFindings,
                    persistedComments
            );

            review.setStatus(ReviewStatus.COMPLETED);
            review.setSummary(summaryBuilder.isEmpty() ? "No issues found." : summaryBuilder.toString().trim());
            review.setPromptTokens(totalPromptTokens);
            review.setCompletionTokens(totalCompletionTokens);
            review.setCompletedAt(Instant.now());
            reviewRepository.save(review);

            reviewEventPublisher.publishReviewCompleted(
                    organizationId,
                    correlationId,
                    new ReviewCompletedPayload(
                            review.getId(),
                            pullRequest.getId(),
                            repository.getId(),
                            ReviewStatus.COMPLETED.name(),
                            deduplicatedFindings.size(),
                            commentsPosted,
                            System.currentTimeMillis() - startedAt
                    )
            );

            auditService.record(
                    organizationId,
                    correlationId,
                    "REVIEW_COMPLETED",
                    "review-engine",
                    "REVIEW",
                    review.getId().toString(),
                    Map.of(
                            "findingsCount", deduplicatedFindings.size(),
                            "commentsPosted", commentsPosted,
                            "pullRequestNumber", payload.pullRequestNumber()
                    ),
                    null
            );
        } catch (Exception ex) {
            review.setStatus(ReviewStatus.FAILED);
            review.setErrorMessage(ex.getMessage());
            review.setCompletedAt(Instant.now());
            reviewRepository.save(review);

            reviewEventPublisher.publishReviewCompleted(
                    organizationId,
                    correlationId,
                    new ReviewCompletedPayload(
                            review.getId(),
                            payload.pullRequestId(),
                            payload.repositoryId(),
                            ReviewStatus.FAILED.name(),
                            0,
                            0,
                            System.currentTimeMillis() - startedAt
                    )
            );
            throw new CodeSageException("Pull request review failed", ex);
        }
    }

    private LlmReviewResponse reviewMethod(String pullRequestTitle, UUID repositoryId, ModifiedMethod method) {
        List<RetrievedContext> contexts = contextRetrievalService.retrieveReviewContext(
                repositoryId,
                method.signature() + "\n" + method.sourceCode(),
                method.filePath(),
                reviewProperties.getContextTopK()
        );
        String relatedContext = contextRetrievalService.buildPromptContext(contexts);
        String userPrompt = reviewPromptBuilder.buildUserPrompt(pullRequestTitle, method, relatedContext);
        return codeReviewLlmService.review(null, userPrompt);
    }

    private List<ReviewFinding> mapFindings(ModifiedMethod method, LlmReviewResponse response) {
        if (response.findings() == null || response.findings().isEmpty()) {
            return List.of();
        }
        List<ReviewFinding> findings = new ArrayList<>();
        for (LlmReviewResponse.LlmFinding finding : response.findings()) {
            if (findings.size() >= reviewProperties.getMaxFindingsPerMethod()) {
                break;
            }
            findings.add(new ReviewFinding(
                    method.filePath(),
                    finding.startLine() != null ? finding.startLine() : method.startLine(),
                    finding.endLine() != null ? finding.endLine() : method.endLine(),
                    parseSeverity(finding.severity()),
                    finding.category() != null ? finding.category().toUpperCase() : "MAINTAINABILITY",
                    finding.message(),
                    finding.suggestion(),
                    0.0
            ));
        }
        return findings;
    }

    private Severity parseSeverity(String severity) {
        try {
            return Severity.valueOf(severity.toUpperCase());
        } catch (Exception ex) {
            return Severity.INFO;
        }
    }

    private List<ReviewCommentEntity> persistFindings(UUID reviewId, List<ReviewFinding> findings) {
        List<ReviewCommentEntity> entities = new ArrayList<>();
        for (ReviewFinding finding : findings) {
            ReviewCommentEntity entity = new ReviewCommentEntity();
            entity.setReviewId(reviewId);
            entity.setFilePath(finding.filePath());
            entity.setStartLine(finding.startLine());
            entity.setEndLine(finding.endLine());
            entity.setSeverity(finding.severity());
            entity.setCategory(finding.category());
            entity.setMessage(finding.message());
            entity.setSuggestion(finding.suggestion());
            entities.add(reviewCommentRepository.save(entity));
        }
        return entities;
    }

    private int publishToScm(
            ReviewPublisher reviewPublisher,
            ScmAccessToken accessToken,
            String owner,
            String repoName,
            int pullRequestNumber,
            String summary,
            List<ReviewFinding> findings,
            List<ReviewCommentEntity> persistedComments
    ) {
        if (findings.isEmpty()) {
            ScmReview review = reviewPublisher.publishReview(
                    accessToken,
                    owner,
                    repoName,
                    pullRequestNumber,
                    new ReviewSubmission(
                            summary.isBlank() ? "CodeSage review completed. No issues found." : summary,
                            List.of(),
                            ReviewSubmission.ReviewEvent.COMMENT
                    )
            );
            log.info("Published summary review {} to SCM", review.externalId());
            return 0;
        }

        List<ReviewCommentRequest> commentRequests = findings.stream()
                .map(finding -> new ReviewCommentRequest(
                        finding.filePath(),
                        finding.startLine(),
                        finding.endLine(),
                        finding.severity(),
                        finding.message(),
                        finding.suggestion()
                ))
                .toList();

        ReviewSubmission.ReviewEvent event = findings.stream().anyMatch(f -> f.severity() == Severity.ERROR)
                ? ReviewSubmission.ReviewEvent.REQUEST_CHANGES
                : ReviewSubmission.ReviewEvent.COMMENT;

        ScmReview review = reviewPublisher.publishReview(
                accessToken,
                owner,
                repoName,
                pullRequestNumber,
                new ReviewSubmission(summary, commentRequests, event)
        );

        for (int index = 0; index < persistedComments.size(); index++) {
            persistedComments.get(index).setExternalCommentId(review.externalId() + ":" + index);
            reviewCommentRepository.save(persistedComments.get(index));
        }

        log.info("Published review {} with {} comments to SCM", review.externalId(), commentRequests.size());
        return commentRequests.size();
    }

    private String[] parseRepositoryCoordinates(String fullName) {
        String[] parts = fullName.split("/");
        if (parts.length != 2) {
            throw new CodeSageException("Invalid repository full name: " + fullName);
        }
        return parts;
    }
}
