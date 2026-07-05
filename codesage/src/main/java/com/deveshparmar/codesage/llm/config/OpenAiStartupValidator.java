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
                    "OpenAI API key is NOT loaded. Set environment variable OPENAI_API_KEY in your IntelliJ "
                            + "Run Configuration (Run -> Edit Configurations -> Environment variables), then restart "
                            + "the app. Terminal/shell exports are not picked up by IntelliJ automatically."
            );
            return;
        }

        String prefix = apiKey.length() <= 8 ? "***" : apiKey.substring(0, 8) + "...";
        log.info("OpenAI API key loaded successfully (prefix: {})", prefix);
    }
}
