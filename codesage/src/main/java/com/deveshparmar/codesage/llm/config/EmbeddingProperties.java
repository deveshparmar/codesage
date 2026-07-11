package com.deveshparmar.codesage.llm.config;

import com.deveshparmar.codesage.llm.domain.EmbeddingProviderType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "codesage.embedding")
public class EmbeddingProperties {

    private EmbeddingProviderType provider = EmbeddingProviderType.OPENAI_COMPATIBLE;
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "text-embedding-3-large";
    private int dimensions = 3072;
    private Duration timeout = Duration.ofSeconds(60);
    private boolean requestDimensions = true;
    private boolean requireApiKey = true;
    private Gemini gemini = new Gemini();
    private HuggingFace huggingFace = new HuggingFace();

    @Getter
    @Setter
    public static class Gemini {
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        private String taskType = "RETRIEVAL_DOCUMENT";
    }

    @Getter
    @Setter
    public static class HuggingFace {
        private String baseUrl = "https://api-inference.huggingface.co";
        private boolean waitForModel = true;
    }
}
