package com.deveshparmar.codesage.llm.config;

import com.deveshparmar.codesage.llm.application.EmbeddingProviderRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingStartupValidator {

    private final EmbeddingProperties embeddingProperties;
    private final EmbeddingProviderRegistry embeddingProviderRegistry;

    @PostConstruct
    void logConfigurationStatus() {
        log.info(
                "Embedding provider configured: provider={}, model={}, dimensions={}",
                embeddingProperties.getProvider(),
                embeddingProviderRegistry.activeProvider().modelName(),
                embeddingProviderRegistry.activeProvider().dimensions()
        );

        if (embeddingProperties.isRequireApiKey()
                && (embeddingProperties.getApiKey() == null || embeddingProperties.getApiKey().isBlank())) {
            log.warn(
                    "Embedding API key is NOT loaded. Set EMBEDDING_API_KEY or codesage.embedding.api-key. "
                            + "For local Ollama, set require-api-key=false."
            );
            return;
        }

        if (embeddingProperties.getApiKey() != null && !embeddingProperties.getApiKey().isBlank()) {
            String prefix = embeddingProperties.getApiKey().length() <= 8
                    ? "***"
                    : embeddingProperties.getApiKey().substring(0, 8) + "...";
            log.info("Embedding API key loaded (prefix: {})", prefix);
        }
    }
}
