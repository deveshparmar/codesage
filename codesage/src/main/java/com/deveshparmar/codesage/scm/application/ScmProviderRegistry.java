package com.deveshparmar.codesage.scm.application;

import com.deveshparmar.codesage.common.domain.ScmProviderType;
import com.deveshparmar.codesage.common.exception.InvalidRequestException;
import com.deveshparmar.codesage.scm.domain.ScmProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ScmProviderRegistry {

    private final Map<ScmProviderType, ScmProvider> providers;

    public ScmProviderRegistry(List<ScmProvider> providerList) {
        this.providers = new EnumMap<>(ScmProviderType.class);
        for (ScmProvider provider : providerList) {
            providers.put(provider.getProviderType(), provider);
        }
    }

    public ScmProvider getProvider(ScmProviderType type) {
        ScmProvider provider = providers.get(type);
        if (provider == null) {
            throw new InvalidRequestException("Unsupported SCM provider: " + type);
        }
        return provider;
    }

    public boolean isSupported(ScmProviderType type) {
        return providers.containsKey(type);
    }
}
