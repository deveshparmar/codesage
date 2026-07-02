package com.deveshparmar.codesage.scm.infrastructure.github;

import com.deveshparmar.codesage.common.domain.ScmProviderType;
import com.deveshparmar.codesage.scm.domain.PullRequestProvider;
import com.deveshparmar.codesage.scm.domain.RepositoryProvider;
import com.deveshparmar.codesage.scm.domain.ReviewPublisher;
import com.deveshparmar.codesage.scm.domain.ScmProvider;
import com.deveshparmar.codesage.scm.domain.WebhookVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GitHubProvider implements ScmProvider {

    private final RepositoryProvider repositoryProvider;
    private final PullRequestProvider pullRequestProvider;
    private final ReviewPublisher reviewPublisher;
    private final WebhookVerifier webhookVerifier;

    public GitHubProvider(
            @Qualifier("githubRestClient") RestClient githubRestClient,
            ObjectMapper objectMapper
    ) {
        this.repositoryProvider = new GitHubRepositoryProvider(githubRestClient);
        this.pullRequestProvider = new GitHubPullRequestProvider(githubRestClient);
        this.reviewPublisher = new GitHubReviewPublisher(githubRestClient);
        this.webhookVerifier = new GitHubWebhookVerifier(objectMapper);
    }

    @Override
    public ScmProviderType getProviderType() {
        return ScmProviderType.GITHUB;
    }

    @Override
    public RepositoryProvider getRepositoryProvider() {
        return repositoryProvider;
    }

    @Override
    public PullRequestProvider getPullRequestProvider() {
        return pullRequestProvider;
    }

    @Override
    public ReviewPublisher getReviewPublisher() {
        return reviewPublisher;
    }

    @Override
    public WebhookVerifier getWebhookVerifier() {
        return webhookVerifier;
    }
}
