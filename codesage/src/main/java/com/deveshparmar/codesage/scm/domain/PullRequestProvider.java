package com.deveshparmar.codesage.scm.domain;

import java.util.List;

public interface PullRequestProvider {

    ScmPullRequest fetchPullRequest(ScmAccessToken token, String owner, String repositoryName, int pullRequestNumber);

    String fetchPullRequestDiff(ScmAccessToken token, String owner, String repositoryName, int pullRequestNumber);

    List<ScmPullRequestFile> fetchChangedFiles(ScmAccessToken token, String owner, String repositoryName, int pullRequestNumber);
}
