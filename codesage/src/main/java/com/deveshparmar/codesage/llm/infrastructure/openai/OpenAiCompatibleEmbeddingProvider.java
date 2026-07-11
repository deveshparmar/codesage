package com.deveshparmar.codesage.llm.infrastructure.openai;

import com.deveshparmar.codesage.common.exception.CodeSageException;
import com.deveshparmar.codesage.llm.config.EmbeddingProperties;
import com.deveshparmar.codesage.llm.domain.EmbeddingProvider;
import com.deveshparmar.codesage.llm.domain.EmbeddingProviderType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;

@Component
public class OpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final EmbeddingProperties embeddingProperties;

    public OpenAiCompatibleEmbeddingProvider(EmbeddingProperties embeddingProperties) {
        this.embeddingProperties = embeddingProperties;
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(embeddingProperties.getBaseUrl());
        if (embeddingProperties.getApiKey() != null && !embeddingProperties.getApiKey().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + embeddingProperties.getApiKey());
        }
        this.restClient = builder.build();
    }

    @Override
    public EmbeddingProviderType providerType() {
        return EmbeddingProviderType.OPENAI_COMPATIBLE;
    }

    @Override
    public List<float[]> embed(List<String> inputs) {
        validateConfiguration();
        if (inputs.isEmpty()) {
            return List.of();
        }

        try {
            EmbeddingResponse response = restClient.post()
                    .uri("/embeddings")
                    .body(buildRequest(inputs))
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new CodeSageException("Embedding response was empty for provider " + providerType());
            }

            return response.data().stream()
                    .sorted(Comparator.comparing(EmbeddingData::index))
                    .map(EmbeddingData::embedding)
                    .map(this::toFloatArray)
                    .map(this::validateVectorLength)
                    .toList();
        } catch (CodeSageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CodeSageException("OpenAI-compatible embedding request failed", ex);
        }
    }

    @Override
    public String modelName() {
        return embeddingProperties.getModel();
    }

    @Override
    public int dimensions() {
        return embeddingProperties.getDimensions();
    }

    private void validateConfiguration() {
        if (embeddingProperties.isRequireApiKey()
                && (embeddingProperties.getApiKey() == null || embeddingProperties.getApiKey().isBlank())) {
            throw new CodeSageException(
                    "Embedding API key is not configured. Set EMBEDDING_API_KEY or codesage.embedding.api-key."
            );
        }
    }

    private EmbeddingRequest buildRequest(List<String> inputs) {
        if (embeddingProperties.isRequestDimensions()) {
            return new EmbeddingRequest(
                    embeddingProperties.getModel(),
                    inputs,
                    embeddingProperties.getDimensions()
            );
        }
        return new EmbeddingRequest(
                embeddingProperties.getModel(),
                inputs,
                null
        );
    }

    private float[] validateVectorLength(float[] vector) {
        if (vector.length != embeddingProperties.getDimensions()) {
            throw new CodeSageException(
                    "Embedding provider returned " + vector.length + " dimensions, expected "
                            + embeddingProperties.getDimensions()
                            + ". Update codesage.embedding.dimensions and the database vector column."
            );
        }
        return vector;
    }

    private float[] toFloatArray(List<Double> values) {
        float[] result = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index).floatValue();
        }
        return result;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record EmbeddingRequest(
            @JsonProperty("model") String model,
            @JsonProperty("input") List<String> input,
            @JsonProperty("dimensions") Integer dimensions
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
