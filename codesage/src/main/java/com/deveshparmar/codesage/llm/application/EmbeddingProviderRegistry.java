package com.deveshparmar.codesage.llm.application;

import com.deveshparmar.codesage.common.exception.InvalidRequestException;
import com.deveshparmar.codesage.llm.config.EmbeddingProperties;
import com.deveshparmar.codesage.llm.domain.EmbeddingModel;
import com.deveshparmar.codesage.llm.domain.EmbeddingProvider;
import com.deveshparmar.codesage.llm.domain.EmbeddingProviderType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class EmbeddingProviderRegistry {

    private final Map<EmbeddingProviderType, EmbeddingProvider> providers;
    private final EmbeddingProvider activeProvider;

    public EmbeddingProviderRegistry(List<EmbeddingProvider> providerList, EmbeddingProperties embeddingProperties) {
        this.providers = new EnumMap<>(EmbeddingProviderType.class);
        for (EmbeddingProvider provider : providerList) {
            providers.put(provider.providerType(), provider);
        }
        this.activeProvider = resolveActiveProvider(embeddingProperties.getProvider());
    }

    public EmbeddingModel activeModel() {
        return activeProvider;
    }

    public EmbeddingProvider activeProvider() {
        return activeProvider;
    }

    public boolean isSupported(EmbeddingProviderType type) {
        return providers.containsKey(type);
    }

    private EmbeddingProvider resolveActiveProvider(EmbeddingProviderType type) {
        EmbeddingProvider provider = providers.get(type);
        if (provider == null) {
            throw new InvalidRequestException("Unsupported embedding provider: " + type);
        }
        return provider;
    }
}
