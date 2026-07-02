package com.deveshparmar.codesage.scm.domain;

public interface ReviewPublisher {

    ScmReviewComment publishReviewComment(
            ScmAccessToken token,
            String owner,
            String repositoryName,
            int pullRequestNumber,
            ReviewCommentRequest request
    );

    ScmReview publishReview(
            ScmAccessToken token,
            String owner,
            String repositoryName,
            int pullRequestNumber,
            ReviewSubmission submission
    );
}
