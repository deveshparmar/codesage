package com.deveshparmar.codesage.scm.domain;

import com.deveshparmar.codesage.common.domain.ScmProviderType;

public interface ScmProvider {

    ScmProviderType getProviderType();

    RepositoryProvider getRepositoryProvider();

    PullRequestProvider getPullRequestProvider();

    ReviewPublisher getReviewPublisher();

    WebhookVerifier getWebhookVerifier();
}
