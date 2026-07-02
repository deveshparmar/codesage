package com.deveshparmar.codesage.indexing.application;

import com.deveshparmar.codesage.indexing.infrastructure.persistence.ChunkEntity;
import com.deveshparmar.codesage.indexing.infrastructure.persistence.ChunkJpaRepository;
import com.deveshparmar.codesage.indexing.infrastructure.persistence.EmbeddingJpaRepository;
import com.deveshparmar.codesage.indexing.infrastructure.persistence.EmbeddingVectorRepository;
import com.deveshparmar.codesage.indexing.infrastructure.redis.EmbeddingCacheService;
import com.deveshparmar.codesage.llm.application.EmbeddingService;
import com.deveshparmar.codesage.platform.infrastructure.kafka.EmbeddingGenerationRequestedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingGenerationService {

    private final ChunkJpaRepository chunkRepository;
    private final EmbeddingJpaRepository embeddingRepository;
    private final EmbeddingVectorRepository embeddingVectorRepository;
    private final EmbeddingService embeddingService;
    private final EmbeddingCacheService embeddingCacheService;

    @Transactional
    public int generateEmbeddings(UUID repositoryId, EmbeddingGenerationRequestedPayload payload) {
        List<ChunkEntity> chunks = chunkRepository.findByRepositoryIdAndChunkIds(repositoryId, payload.chunkIds());
        if (chunks.isEmpty()) {
            return 0;
        }

        List<ChunkEntity> pendingChunks = chunks.stream()
                .filter(chunk -> !embeddingRepository.existsByChunkId(chunk.getId()))
                .toList();
        if (pendingChunks.isEmpty()) {
            return 0;
        }

        List<ChunkEntity> chunksToEmbed = new ArrayList<>();
        List<String> texts = new ArrayList<>();

        for (ChunkEntity chunk : pendingChunks) {
            var cached = embeddingCacheService.get(chunk.getChunkHash());
            if (cached.isPresent()) {
                embeddingVectorRepository.upsertEmbedding(chunk.getId(), cached.get(), payload.model());
            } else {
                chunksToEmbed.add(chunk);
                texts.add(buildEmbeddingInput(chunk));
            }
        }

        if (!chunksToEmbed.isEmpty()) {
            List<float[]> embeddings = embeddingService.generateEmbeddings(texts);
            for (int index = 0; index < chunksToEmbed.size(); index++) {
                ChunkEntity chunk = chunksToEmbed.get(index);
                float[] vector = embeddings.get(index);
                embeddingVectorRepository.upsertEmbedding(chunk.getId(), vector, payload.model());
                embeddingCacheService.put(chunk.getChunkHash(), vector);
            }
        }

        log.info("Generated embeddings for {} chunks in repository {}", pendingChunks.size(), repositoryId);
        return pendingChunks.size();
    }

    private String buildEmbeddingInput(ChunkEntity chunk) {
        return """
                File: %s
                Package: %s
                Class: %s
                Method: %s
                Type: %s
                
                %s
                """.formatted(
                chunk.getMetadata().getOrDefault("file", ""),
                chunk.getPackageName() != null ? chunk.getPackageName() : "",
                chunk.getClassName() != null ? chunk.getClassName() : "",
                chunk.getMethodName() != null ? chunk.getMethodName() : "",
                chunk.getChunkType(),
                chunk.getContent()
        );
    }
}
