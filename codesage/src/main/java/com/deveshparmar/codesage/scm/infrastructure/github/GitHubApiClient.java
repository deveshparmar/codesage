package com.deveshparmar.codesage.scm.infrastructure.github;

import com.deveshparmar.codesage.common.exception.CodeSageException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

class GitHubApiClient {

    private final RestClient restClient;

    GitHubApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    <T> T get(String path, String token, Class<T> responseType) {
        try {
            return restClient.get()
                    .uri(path)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(responseType);
        } catch (Exception ex) {
            throw new CodeSageException("GitHub API GET failed for path: " + path, ex);
        }
    }

    <T> T get(String path, String token, ParameterizedTypeReference<T> responseType) {
        try {
            return restClient.get()
                    .uri(path)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(responseType);
        } catch (Exception ex) {
            throw new CodeSageException("GitHub API GET failed for path: " + path, ex);
        }
    }

    String getRaw(String path, String token) {
        try {
            return restClient.get()
                    .uri(path)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github.diff")
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            throw new CodeSageException("GitHub API GET (raw) failed for path: " + path, ex);
        }
    }

    <T> T post(String path, String token, Object body, Class<T> responseType) {
        try {
            return restClient.post()
                    .uri(path)
                    .header("Authorization", "Bearer " + token)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (Exception ex) {
            throw new CodeSageException("GitHub API POST failed for path: " + path, ex);
        }
    }
}
