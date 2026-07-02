package com.deveshparmar.codesage.platform.infrastructure.redis;

import com.deveshparmar.codesage.common.exception.InvalidRequestException;
import com.deveshparmar.codesage.platform.config.CodeSageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String KEY_PREFIX = "rate-limit:org:";

    private final StringRedisTemplate redisTemplate;
    private final CodeSageProperties codeSageProperties;

    public void checkRateLimit(UUID organizationId) {
        String key = KEY_PREFIX + organizationId;
        Duration window = codeSageProperties.getRedis().getRateLimitWindow();
        int maxRequests = codeSageProperties.getRedis().getRateLimitMaxRequests();

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        if (count != null && count > maxRequests) {
            throw new InvalidRequestException("Rate limit exceeded. Maximum %d requests per %s."
                    .formatted(maxRequests, window));
        }
    }
}
