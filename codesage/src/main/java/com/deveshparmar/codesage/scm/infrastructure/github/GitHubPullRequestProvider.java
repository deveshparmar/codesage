package com.deveshparmar.codesage.scm.infrastructure.github;

import com.deveshparmar.codesage.scm.domain.PullRequestProvider;
import com.deveshparmar.codesage.scm.domain.ScmAccessToken;
import com.deveshparmar.codesage.scm.domain.ScmPullRequest;
import com.deveshparmar.codesage.scm.domain.ScmPullRequestFile;
import com.deveshparmar.codesage.scm.infrastructure.github.dto.GitHubPullRequestFileResponse;
import com.deveshparmar.codesage.scm.infrastructure.github.dto.GitHubPullRequestResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

class GitHubPullRequestProvider implements PullRequestProvider {

    private final GitHubApiClient apiClient;

    GitHubPullRequestProvider(RestClient restClient) {
        this.apiClient = new GitHubApiClient(restClient);
    }

    @Override
    public ScmPullRequest fetchPullRequest(ScmAccessToken token, String owner, String repositoryName, int pullRequestNumber) {
        String path = "/repos/%s/%s/pulls/%d".formatted(owner, repositoryName, pullRequestNumber);
        GitHubPullRequestResponse response = apiClient.get(path, token.value(), GitHubPullRequestResponse.class);
        return GitHubPullRequestMapper.toScmPullRequest(response);
    }

    @Override
    public String fetchPullRequestDiff(ScmAccessToken token, String owner, String repositoryName, int pullRequestNumber) {
        String path = "/repos/%s/%s/pulls/%d".formatted(owner, repositoryName, pullRequestNumber);
        return apiClient.getRaw(path, token.value());
    }

    @Override
    public List<ScmPullRequestFile> fetchChangedFiles(ScmAccessToken token, String owner, String repositoryName, int pullRequestNumber) {
        String path = "/repos/%s/%s/pulls/%d/files?per_page=100".formatted(owner, repositoryName, pullRequestNumber);
        List<GitHubPullRequestFileResponse> responses = apiClient.get(
                path,
                token.value(),
                new ParameterizedTypeReference<>() {}
        );
        return responses.stream()
                .map(file -> new ScmPullRequestFile(
                        file.filename(),
                        file.status(),
                        file.additions(),
                        file.deletions(),
                        file.changes(),
                        file.patch()
                ))
                .toList();
    }
}
