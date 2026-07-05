package com.deveshparmar.codesage.llm.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiStartupValidator {

    private final OpenAiProperties openAiProperties;

    @PostConstruct
    void logConfigurationStatus() {
        String apiKey = openAiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn(
                    "Chat LLM API key is NOT loaded. Set OPENAI_API_KEY for PR review generation."
            );
            return;
        }

        String prefix = apiKey.length() <= 8 ? "***" : apiKey.substring(0, 8) + "...";
        log.info("Chat LLM API key loaded successfully (prefix: {})", prefix);
    }
}
