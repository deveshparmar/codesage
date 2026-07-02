package com.deveshparmar.codesage.indexing.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmbeddingCacheService {

    private static final String KEY_PREFIX = "embedding:cache:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<float[]> get(String chunkHash) {
        String cached = redisTemplate.opsForValue().get(KEY_PREFIX + chunkHash);
        if (cached == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(cached, float[].class));
        } catch (JsonProcessingException ex) {
            return Optional.empty();
        }
    }

    public void put(String chunkHash, float[] embedding) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + chunkHash, objectMapper.writeValueAsString(embedding), TTL);
        } catch (JsonProcessingException ex) {
            // Skip cache write failures.
        }
    }
}
