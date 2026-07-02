package com.deveshparmar.codesage.rag.application;

import com.deveshparmar.codesage.common.domain.ChunkType;
import com.deveshparmar.codesage.rag.domain.RetrievedContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContextRetrievalService {

    private final SemanticSearchService semanticSearchService;

    public List<RetrievedContext> retrieveReviewContext(
            UUID repositoryId,
            String changedMethodSignature,
            String filePath,
            int topK
    ) {
        return semanticSearchService.search(
                repositoryId,
                changedMethodSignature,
                topK,
                null,
                ChunkType.METHOD,
                null,
                null,
                filePath
        );
    }

    public String buildPromptContext(List<RetrievedContext> contexts) {
        if (contexts.isEmpty()) {
            return "No related context found.";
        }
        return contexts.stream()
                .map(context -> """
                        --- Context (similarity=%.3f) ---
                        File: %s
                        Class: %s
                        Method: %s
                        Lines: %d-%d
                        
                        %s
                        """.formatted(
                        context.similarity(),
                        context.filePath(),
                        context.className(),
                        context.methodName(),
                        context.startLine(),
                        context.endLine(),
                        context.content()
                ))
                .collect(Collectors.joining("\n"));
    }
}
