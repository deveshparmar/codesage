package com.deveshparmar.codesage.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "codesage")
public class CodeSageProperties {

    private Kafka kafka = new Kafka();
    private Redis redis = new Redis();
    private Security security = new Security();

    @Getter
    @Setter
    public static class Kafka {
        private Topics topics = new Topics();
    }

    @Getter
    @Setter
    public static class Topics {
        private String repositoryIndexRequested = "codesage.repository.index.requested";
        private String repositoryIndexCompleted = "codesage.repository.index.completed";
        private String embeddingGenerationRequested = "codesage.embedding.generation.requested";
        private String reviewRequested = "codesage.review.requested";
        private String reviewCompleted = "codesage.review.completed";
        private String auditEvents = "codesage.audit.events";
    }

    @Getter
    @Setter
    public static class Redis {
        private Duration webhookDedupTtl = Duration.ofHours(24);
        private Duration rateLimitWindow = Duration.ofMinutes(1);
        private int rateLimitMaxRequests = 100;
    }

    @Getter
    @Setter
    public static class Security {
        private String apiKeyHeader = "X-API-Key";
    }
}
