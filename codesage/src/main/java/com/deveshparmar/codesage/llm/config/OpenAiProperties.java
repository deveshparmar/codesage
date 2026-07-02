package com.deveshparmar.codesage.llm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "codesage.openai")
public class OpenAiProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String embeddingModel = "text-embedding-3-large";
    private int embeddingDimensions = 3072;
    private String chatModel = "gpt-4.1";
    private int chatMaxTokens = 4096;
    private Duration timeout = Duration.ofSeconds(60);
}
