package com.deveshparmar.codesage.scm.infrastructure.github;

import com.deveshparmar.codesage.scm.domain.RepositoryProvider;
import com.deveshparmar.codesage.scm.domain.ScmAccessToken;
import com.deveshparmar.codesage.scm.domain.ScmRepository;
import com.deveshparmar.codesage.scm.infrastructure.github.dto.GitHubRepositoryResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

class GitHubRepositoryProvider implements RepositoryProvider {

    private final GitHubApiClient apiClient;

    GitHubRepositoryProvider(RestClient restClient) {
        this.apiClient = new GitHubApiClient(restClient);
    }

    @Override
    public ScmRepository fetchRepository(ScmAccessToken token, String owner, String repositoryName) {
        String path = "/repos/%s/%s".formatted(owner, repositoryName);
        GitHubRepositoryResponse response = apiClient.get(path, token.value(), GitHubRepositoryResponse.class);
        return GitHubMapper.toScmRepository(response);
    }

    @Override
    public List<ScmRepository> listRepositories(ScmAccessToken token, String owner) {
        String path = "/orgs/%s/repos?per_page=100".formatted(owner);
        List<GitHubRepositoryResponse> responses = apiClient.get(
                path,
                token.value(),
                new ParameterizedTypeReference<>() {}
        );
        return responses.stream()
                .map(GitHubMapper::toScmRepository)
                .toList();
    }

    @Override
    public String getDefaultBranch(ScmAccessToken token, String owner, String repositoryName) {
        return fetchRepository(token, owner, repositoryName).defaultBranch();
    }
}
