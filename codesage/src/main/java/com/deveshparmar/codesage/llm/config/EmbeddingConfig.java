package com.deveshparmar.codesage.llm.config;

import com.deveshparmar.codesage.llm.application.EmbeddingProviderRegistry;
import com.deveshparmar.codesage.llm.domain.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Bean
    EmbeddingModel embeddingModel(EmbeddingProviderRegistry embeddingProviderRegistry) {
        return embeddingProviderRegistry.activeModel();
    }
}
