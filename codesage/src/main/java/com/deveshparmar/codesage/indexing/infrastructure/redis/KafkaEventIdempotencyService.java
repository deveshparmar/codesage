package com.deveshparmar.codesage.indexing.infrastructure.redis;

import com.deveshparmar.codesage.platform.config.CodeSageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KafkaEventIdempotencyService {

    private static final String KEY_PREFIX = "kafka:event:";

    private final StringRedisTemplate redisTemplate;
    private final CodeSageProperties codeSageProperties;

    public boolean alreadyProcessed(UUID eventId) {
        String key = KEY_PREFIX + eventId;
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(
                key,
                "1",
                codeSageProperties.getRedis().getWebhookDedupTtl()
        );
        return wasAbsent == null || !wasAbsent;
    }

    public void release(UUID eventId) {
        redisTemplate.delete(KEY_PREFIX + eventId);
    }
}
