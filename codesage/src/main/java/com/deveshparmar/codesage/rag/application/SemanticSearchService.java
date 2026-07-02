package com.deveshparmar.codesage.rag.application;

import com.deveshparmar.codesage.common.domain.ChunkType;
import com.deveshparmar.codesage.llm.application.EmbeddingService;
import com.deveshparmar.codesage.rag.config.RagProperties;
import com.deveshparmar.codesage.rag.domain.RetrievedContext;
import com.deveshparmar.codesage.rag.domain.SimilaritySearchQuery;
import com.deveshparmar.codesage.rag.domain.VectorSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final VectorSearchPort vectorSearchPort;
    private final EmbeddingService embeddingService;
    private final RagProperties ragProperties;

    public List<RetrievedContext> search(
            UUID repositoryId,
            String query,
            Integer topK,
            Double minSimilarity,
            ChunkType chunkType,
            String packageName,
            String className,
            String filePath
    ) {
        float[] queryVector = embeddingService.generateEmbedding(query);
        SimilaritySearchQuery searchQuery = new SimilaritySearchQuery(
                repositoryId,
                queryVector,
                topK != null ? topK : ragProperties.getDefaultTopK(),
                minSimilarity != null ? minSimilarity : ragProperties.getMinSimilarity(),
                chunkType,
                packageName,
                className,
                filePath
        );
        return vectorSearchPort.search(searchQuery);
    }
}
