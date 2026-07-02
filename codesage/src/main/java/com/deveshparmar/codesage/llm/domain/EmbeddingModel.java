package com.deveshparmar.codesage.llm.domain;

import java.util.List;

public interface EmbeddingModel {

    List<float[]> embed(List<String> inputs);

    String modelName();

    int dimensions();
}
