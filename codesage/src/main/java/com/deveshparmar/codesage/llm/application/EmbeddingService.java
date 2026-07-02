package com.deveshparmar.codesage.llm.application;

import com.deveshparmar.codesage.llm.domain.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public List<float[]> generateEmbeddings(List<String> texts) {
        return embeddingModel.embed(texts);
    }

    public float[] generateEmbedding(String text) {
        return embeddingModel.embed(List.of(text)).getFirst();
    }

    public String modelName() {
        return embeddingModel.modelName();
    }
}
