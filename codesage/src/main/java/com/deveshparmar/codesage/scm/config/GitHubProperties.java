package com.deveshparmar.codesage.scm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "codesage.github")
public class GitHubProperties {

    private String apiBaseUrl = "https://api.github.com";
    private String webhookSecretHeader = "X-Hub-Signature-256";
    private String deliveryIdHeader = "X-GitHub-Delivery";
    private String eventHeader = "X-GitHub-Event";
}
