package com.deveshparmar.codesage.scm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ScmConfig {

    @Bean("githubRestClient")
    RestClient githubRestClient(GitHubProperties gitHubProperties) {
        return RestClient.builder()
                .baseUrl(gitHubProperties.getApiBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }
}
