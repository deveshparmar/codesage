package com.deveshparmar.codesage.llm.infrastructure.openai;

import com.deveshparmar.codesage.common.exception.CodeSageException;
import com.deveshparmar.codesage.llm.config.OpenAiProperties;
import com.deveshparmar.codesage.llm.domain.CodeReviewLlmPort;
import com.deveshparmar.codesage.review.domain.LlmReviewResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OpenAiCodeReviewClient implements CodeReviewLlmPort {

    private static final String SYSTEM_PROMPT = """
            You are CodeSage, an expert software engineer performing pull request code reviews.
            Analyze code changes for bugs, security vulnerabilities, performance issues, and maintainability concerns.
            Respond ONLY with valid JSON matching this schema:
            {
              "summary": "brief overall assessment",
              "findings": [
                {
                  "severity": "INFO|WARNING|ERROR",
                  "category": "BUG|SECURITY|PERFORMANCE|MAINTAINABILITY|STYLE",
                  "message": "clear description of the issue",
                  "suggestion": "actionable fix recommendation",
                  "startLine": 0,
                  "endLine": 0
                }
              ]
            }
            Return an empty findings array if no issues are found.
            """;

    private final RestClient restClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    public OpenAiCodeReviewClient(OpenAiProperties openAiProperties, ObjectMapper objectMapper) {
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(openAiProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + openAiProperties.getApiKey())
                .build();
    }

    @Override
    public LlmReviewResponse reviewCode(String systemPrompt, String userPrompt) {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            throw new CodeSageException(
                    "OpenAI API key is not configured. Set OPENAI_API_KEY in IntelliJ Run Configuration "
                            + "environment variables and restart the application."
            );
        }

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(new ChatCompletionRequest(
                            openAiProperties.getChatModel(),
                            List.of(
                                    new ChatMessage("system", systemPrompt != null ? systemPrompt : SYSTEM_PROMPT),
                                    new ChatMessage("user", userPrompt)
                            ),
                            openAiProperties.getChatMaxTokens(),
                            new ResponseFormat("json_object")
                    ))
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new CodeSageException("OpenAI chat completion returned empty response");
            }

            String content = response.choices().getFirst().message().content();
            LlmReviewResponse parsed = objectMapper.readValue(content, LlmReviewResponse.class);
            int promptTokens = response.usage() != null ? response.usage().promptTokens() : 0;
            int completionTokens = response.usage() != null ? response.usage().completionTokens() : 0;
            return new LlmReviewResponse(parsed.summary(), parsed.findings(), promptTokens, completionTokens);
        } catch (CodeSageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CodeSageException("OpenAI code review request failed", ex);
        }
    }

    private record ChatCompletionRequest(
            @JsonProperty("model") String model,
            @JsonProperty("messages") List<ChatMessage> messages,
            @JsonProperty("max_tokens") int maxTokens,
            @JsonProperty("response_format") ResponseFormat responseFormat
    ) {
    }

    private record ChatMessage(
            @JsonProperty("role") String role,
            @JsonProperty("content") String content
    ) {
    }

    private record ResponseFormat(@JsonProperty("type") String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponse(
            @JsonProperty("choices") List<Choice> choices,
            @JsonProperty("usage") Usage usage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(@JsonProperty("message") ChoiceMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChoiceMessage(@JsonProperty("content") String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens
    ) {
    }
}
