package com.deveshparmar.codesage;

import com.deveshparmar.codesage.llm.config.EmbeddingProperties;
import com.deveshparmar.codesage.llm.domain.EmbeddingProviderType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = {
        "codesage.embedding.provider=OPENAI_COMPATIBLE",
        "codesage.embedding.model=test-model",
        "codesage.embedding.dimensions=384",
        "codesage.embedding.api-key=test-key",
        "codesage.embedding.require-api-key=false"
})
class EmbeddingProviderRegistryTest {

    @Autowired
    private EmbeddingProperties embeddingProperties;

    @Test
    void loadsConfiguredEmbeddingProvider() {
        assertEquals(EmbeddingProviderType.OPENAI_COMPATIBLE, embeddingProperties.getProvider());
        assertEquals("test-model", embeddingProperties.getModel());
        assertEquals(384, embeddingProperties.getDimensions());
    }
}
