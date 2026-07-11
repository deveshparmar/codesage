package com.deveshparmar.codesage.llm.infrastructure.huggingface;

import com.deveshparmar.codesage.common.exception.CodeSageException;
import com.deveshparmar.codesage.llm.config.EmbeddingProperties;
import com.deveshparmar.codesage.llm.domain.EmbeddingProvider;
import com.deveshparmar.codesage.llm.domain.EmbeddingProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class HuggingFaceEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final EmbeddingProperties embeddingProperties;

    public HuggingFaceEmbeddingProvider(EmbeddingProperties embeddingProperties) {
        this.embeddingProperties = embeddingProperties;
        this.restClient = RestClient.builder()
                .baseUrl(embeddingProperties.getHuggingFace().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + embeddingProperties.getApiKey())
                .build();
    }

    @Override
    public EmbeddingProviderType providerType() {
        return EmbeddingProviderType.HUGGINGFACE;
    }

    @Override
    public List<float[]> embed(List<String> inputs) {
        validateConfiguration();
        if (inputs.isEmpty()) {
            return List.of();
        }

        try {
            JsonNode response = restClient.post()
                    .uri("/pipeline/feature-extraction/{model}", embeddingProperties.getModel())
                    .header("x-wait-for-model", String.valueOf(embeddingProperties.getHuggingFace().isWaitForModel()))
                    .body(inputs.size() == 1 ? inputs.getFirst() : inputs)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new CodeSageException("Hugging Face embedding response was empty");
            }

            return parseResponse(response);
        } catch (CodeSageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CodeSageException("Hugging Face embedding request failed", ex);
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
                    "Hugging Face API token is not configured. Set EMBEDDING_API_KEY or codesage.embedding.api-key."
            );
        }
    }

    private List<float[]> parseResponse(JsonNode response) {
        if (response.isArray() && !response.isEmpty()) {
            if (response.get(0).isArray() && response.get(0).get(0).isNumber()) {
                return parseBatchTokenMatrix(response);
            }
            if (response.get(0).isNumber()) {
                return List.of(validateVectorLength(toFloatArray(response)));
            }
            if (response.get(0).isArray() && response.get(0).get(0).isArray()) {
                return parseBatchTokenMatrix(response);
            }
        }
        throw new CodeSageException("Unexpected Hugging Face embedding response format");
    }

    private List<float[]> parseBatchTokenMatrix(JsonNode response) {
        List<float[]> vectors = new ArrayList<>();
        for (JsonNode item : response) {
            vectors.add(validateVectorLength(meanPoolTokenEmbeddings(item)));
        }
        return vectors;
    }

    private float[] meanPoolTokenEmbeddings(JsonNode tokenEmbeddings) {
        if (!tokenEmbeddings.isArray() || tokenEmbeddings.isEmpty()) {
            throw new CodeSageException("Hugging Face returned empty token embeddings");
        }

        if (tokenEmbeddings.get(0).isNumber()) {
            return toFloatArray(tokenEmbeddings);
        }

        int dimensions = tokenEmbeddings.get(0).size();
        float[] pooled = new float[dimensions];
        int tokenCount = tokenEmbeddings.size();

        for (JsonNode token : tokenEmbeddings) {
            for (int index = 0; index < dimensions; index++) {
                pooled[index] += token.get(index).floatValue();
            }
        }

        for (int index = 0; index < dimensions; index++) {
            pooled[index] /= tokenCount;
        }
        return pooled;
    }

    private float[] validateVectorLength(float[] vector) {
        if (vector.length != embeddingProperties.getDimensions()) {
            throw new CodeSageException(
                    "Hugging Face returned " + vector.length + " dimensions, expected "
                            + embeddingProperties.getDimensions()
                            + ". Update codesage.embedding.model and codesage.embedding.dimensions."
            );
        }
        return vector;
    }

    private float[] toFloatArray(JsonNode values) {
        float[] result = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index).floatValue();
        }
        return result;
    }
}
