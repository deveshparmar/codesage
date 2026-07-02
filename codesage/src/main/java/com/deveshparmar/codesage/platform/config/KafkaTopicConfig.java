package com.deveshparmar.codesage.platform.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic repositoryIndexRequestedTopic(CodeSageProperties properties) {
        return buildTopic(properties.getKafka().getTopics().getRepositoryIndexRequested());
    }

    @Bean
    NewTopic repositoryIndexCompletedTopic(CodeSageProperties properties) {
        return buildTopic(properties.getKafka().getTopics().getRepositoryIndexCompleted());
    }

    @Bean
    NewTopic embeddingGenerationRequestedTopic(CodeSageProperties properties) {
        return buildTopic(properties.getKafka().getTopics().getEmbeddingGenerationRequested());
    }

    @Bean
    NewTopic reviewRequestedTopic(CodeSageProperties properties) {
        return buildTopic(properties.getKafka().getTopics().getReviewRequested());
    }

    @Bean
    NewTopic reviewCompletedTopic(CodeSageProperties properties) {
        return buildTopic(properties.getKafka().getTopics().getReviewCompleted());
    }

    @Bean
    NewTopic auditEventsTopic(CodeSageProperties properties) {
        return buildTopic(properties.getKafka().getTopics().getAuditEvents());
    }

    private NewTopic buildTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
