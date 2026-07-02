package com.deveshparmar.codesage.platform.infrastructure.redis;

import com.deveshparmar.codesage.platform.config.CodeSageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class WebhookDeduplicationService {

    private static final String KEY_PREFIX = "webhook:delivery:";

    private final StringRedisTemplate redisTemplate;
    private final CodeSageProperties codeSageProperties;

    public boolean isDuplicate(String deliveryId) {
        String key = KEY_PREFIX + deliveryId;
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(
                key,
                "1",
                codeSageProperties.getRedis().getWebhookDedupTtl()
        );
        return wasAbsent == null || !wasAbsent;
    }
}
