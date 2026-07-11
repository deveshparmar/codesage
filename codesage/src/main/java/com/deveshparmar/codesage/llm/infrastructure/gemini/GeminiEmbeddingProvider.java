package com.deveshparmar.codesage.llm.infrastructure.gemini;

import com.deveshparmar.codesage.common.exception.CodeSageException;
import com.deveshparmar.codesage.llm.config.EmbeddingProperties;
import com.deveshparmar.codesage.llm.domain.EmbeddingProvider;
import com.deveshparmar.codesage.llm.domain.EmbeddingProviderType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Component
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final EmbeddingProperties embeddingProperties;

    public GeminiEmbeddingProvider(EmbeddingProperties embeddingProperties) {
        this.embeddingProperties = embeddingProperties;
        this.restClient = RestClient.builder()
                .baseUrl(embeddingProperties.getGemini().getBaseUrl())
                .build();
    }

    @Override
    public EmbeddingProviderType providerType() {
        return EmbeddingProviderType.GEMINI;
    }

    @Override
    public List<float[]> embed(List<String> inputs) {
        validateConfiguration();
        if (inputs.isEmpty()) {
            return List.of();
        }

        try {
            String uri = UriComponentsBuilder.fromPath("/models/{model}:batchEmbedContents")
                    .queryParam("key", embeddingProperties.getApiKey())
                    .buildAndExpand(embeddingProperties.getModel())
                    .toUriString();

            BatchEmbedRequest request = new BatchEmbedRequest(
                    inputs.stream()
                            .map(text -> new EmbedRequest(
                                    "models/" + normalizeModelName(embeddingProperties.getModel()),
                                    new Content(List.of(new Part(text))),
                                    embeddingProperties.getGemini().getTaskType()
                            ))
                            .toList()
            );

            BatchEmbedResponse response = restClient.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(BatchEmbedResponse.class);

            if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
                throw new CodeSageException("Gemini embedding response was empty");
            }

            List<float[]> vectors = new ArrayList<>(response.embeddings().size());
            for (EmbeddingValue embeddingValue : response.embeddings()) {
                if (embeddingValue.values() == null || embeddingValue.values().isEmpty()) {
                    throw new CodeSageException("Gemini returned an empty embedding vector");
                }
                vectors.add(validateVectorLength(toFloatArray(embeddingValue.values())));
            }
            return vectors;
        } catch (CodeSageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CodeSageException("Gemini embedding request failed", ex);
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
        if (embeddingProperties.getApiKey() == null || embeddingProperties.getApiKey().isBlank()) {
            throw new CodeSageException(
                    "Gemini API key is not configured. Set EMBEDDING_API_KEY or codesage.embedding.api-key."
            );
        }
    }

    private String normalizeModelName(String model) {
        return model.startsWith("models/") ? model.substring("models/".length()) : model;
    }

    private float[] validateVectorLength(float[] vector) {
        if (vector.length != embeddingProperties.getDimensions()) {
            throw new CodeSageException(
                    "Gemini returned " + vector.length + " dimensions, expected "
                            + embeddingProperties.getDimensions()
                            + ". Update codesage.embedding.dimensions and reindex."
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

    private record BatchEmbedRequest(@JsonProperty("requests") List<EmbedRequest> requests) {
    }

    private record EmbedRequest(
            @JsonProperty("model") String model,
            @JsonProperty("content") Content content,
            @JsonProperty("taskType") String taskType
    ) {
    }

    private record Content(@JsonProperty("parts") List<Part> parts) {
    }

    private record Part(@JsonProperty("text") String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BatchEmbedResponse(@JsonProperty("embeddings") List<EmbeddingValue> embeddings) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbeddingValue(@JsonProperty("values") List<Double> values) {
    }
}
