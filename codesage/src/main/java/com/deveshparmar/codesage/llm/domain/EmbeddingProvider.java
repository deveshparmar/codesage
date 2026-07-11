package com.deveshparmar.codesage.llm.domain;

public interface EmbeddingProvider extends EmbeddingModel {

    EmbeddingProviderType providerType();
}
