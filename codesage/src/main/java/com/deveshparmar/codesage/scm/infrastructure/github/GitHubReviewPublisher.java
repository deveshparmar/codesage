package com.deveshparmar.codesage.scm.infrastructure.github;

import com.deveshparmar.codesage.scm.domain.ReviewCommentRequest;
import com.deveshparmar.codesage.scm.domain.ReviewPublisher;
import com.deveshparmar.codesage.scm.domain.ReviewSubmission;
import com.deveshparmar.codesage.scm.domain.ScmAccessToken;
import com.deveshparmar.codesage.scm.domain.ScmReview;
import com.deveshparmar.codesage.scm.domain.ScmReviewComment;
import com.deveshparmar.codesage.scm.infrastructure.github.dto.GitHubCreateReviewCommentRequest;
import com.deveshparmar.codesage.scm.infrastructure.github.dto.GitHubCreateReviewRequest;
import com.deveshparmar.codesage.scm.infrastructure.github.dto.GitHubReviewCommentInput;
import com.deveshparmar.codesage.scm.infrastructure.github.dto.GitHubReviewCommentResponse;
import com.deveshparmar.codesage.scm.infrastructure.github.dto.GitHubReviewResponse;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

class GitHubReviewPublisher implements ReviewPublisher {

    private final GitHubApiClient apiClient;

    GitHubReviewPublisher(RestClient restClient) {
        this.apiClient = new GitHubApiClient(restClient);
    }

    @Override
    public ScmReviewComment publishReviewComment(
            ScmAccessToken token,
            String owner,
            String repositoryName,
            int pullRequestNumber,
            ReviewCommentRequest request
    ) {
        String path = "/repos/%s/%s/pulls/%d/comments".formatted(owner, repositoryName, pullRequestNumber);
        GitHubCreateReviewCommentRequest body = new GitHubCreateReviewCommentRequest(
                formatCommentBody(request),
                null,
                request.filePath(),
                request.endLine(),
                "RIGHT"
        );
        GitHubReviewCommentResponse response = apiClient.post(path, token.value(), body, GitHubReviewCommentResponse.class);
        return new ScmReviewComment(
                String.valueOf(response.id()),
                response.path(),
                response.line(),
                response.body(),
                parseInstant(response.createdAt())
        );
    }

    @Override
    public ScmReview publishReview(
            ScmAccessToken token,
            String owner,
            String repositoryName,
            int pullRequestNumber,
            ReviewSubmission submission
    ) {
        String path = "/repos/%s/%s/pulls/%d/reviews".formatted(owner, repositoryName, pullRequestNumber);
        List<GitHubReviewCommentInput> comments = submission.comments().stream()
                .map(comment -> new GitHubReviewCommentInput(
                        comment.filePath(),
                        comment.endLine(),
                        formatCommentBody(comment),
                        "RIGHT"
                ))
                .toList();
        GitHubCreateReviewRequest body = new GitHubCreateReviewRequest(
                submission.summary(),
                mapReviewEvent(submission.event()),
                comments
        );
        GitHubReviewResponse response = apiClient.post(path, token.value(), body, GitHubReviewResponse.class);
        return new ScmReview(
                String.valueOf(response.id()),
                response.body(),
                response.state(),
                parseInstant(response.submittedAt())
        );
    }

    private String formatCommentBody(ReviewCommentRequest request) {
        StringBuilder body = new StringBuilder();
        body.append("**").append(request.severity()).append("** — ").append(request.message());
        if (request.suggestion() != null && !request.suggestion().isBlank()) {
            body.append("\n\n**Suggestion:** ").append(request.suggestion());
        }
        return body.toString();
    }

    private String mapReviewEvent(ReviewSubmission.ReviewEvent event) {
        return switch (event) {
            case COMMENT -> "COMMENT";
            case APPROVE -> "APPROVE";
            case REQUEST_CHANGES -> "REQUEST_CHANGES";
        };
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        return Instant.parse(value);
    }
}
