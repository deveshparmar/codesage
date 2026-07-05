package com.deveshparmar.codesage.llm.infrastructure.openai;

import com.deveshparmar.codesage.common.exception.CodeSageException;
import com.deveshparmar.codesage.llm.config.OpenAiProperties;
import com.deveshparmar.codesage.llm.domain.EmbeddingModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;

@Component
public class OpenAiEmbeddingModel implements EmbeddingModel {

    private final RestClient restClient;
    private final OpenAiProperties openAiProperties;

    public OpenAiEmbeddingModel(OpenAiProperties openAiProperties) {
        this.openAiProperties = openAiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(openAiProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + openAiProperties.getApiKey())
                .build();
    }

    @Override
    public List<float[]> embed(List<String> inputs) {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            throw new CodeSageException(
                    "OpenAI API key is not configured. Set OPENAI_API_KEY in IntelliJ Run Configuration "
                            + "environment variables and restart the application."
            );
        }
        if (inputs.isEmpty()) {
            return List.of();
        }

        try {
            EmbeddingResponse response = restClient.post()
                    .uri("/embeddings")
                    .body(new EmbeddingRequest(
                            openAiProperties.getEmbeddingModel(),
                            inputs,
                            openAiProperties.getEmbeddingDimensions()
                    ))
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null || response.data() == null) {
                throw new CodeSageException("OpenAI embedding response was empty");
            }

            return response.data().stream()
                    .sorted(Comparator.comparing(EmbeddingData::index))
                    .map(EmbeddingData::embedding)
                    .map(this::toFloatArray)
                    .toList();
        } catch (CodeSageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CodeSageException("OpenAI embedding request failed", ex);
        }
    }

    @Override
    public String modelName() {
        return openAiProperties.getEmbeddingModel();
    }

    @Override
    public int dimensions() {
        return openAiProperties.getEmbeddingDimensions();
    }

    private float[] toFloatArray(List<Double> values) {
        float[] result = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index).floatValue();
        }
        return result;
    }

    private record EmbeddingRequest(
            @JsonProperty("model") String model,
            @JsonProperty("input") List<String> input,
            @JsonProperty("dimensions") int dimensions
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbeddingResponse(
            @JsonProperty("data") List<EmbeddingData> data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbeddingData(
            @JsonProperty("index") int index,
            @JsonProperty("embedding") List<Double> embedding
    ) {
    }
}
