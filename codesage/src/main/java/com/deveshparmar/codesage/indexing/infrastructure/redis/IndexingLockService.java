package com.deveshparmar.codesage.indexing.infrastructure.redis;

import com.deveshparmar.codesage.indexing.config.IndexingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IndexingLockService {

    private static final String LOCK_PREFIX = "indexing:lock:";

    private final StringRedisTemplate redisTemplate;
    private final IndexingProperties indexingProperties;

    public boolean tryAcquire(UUID repositoryId) {
        String key = LOCK_PREFIX + repositoryId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", indexingProperties.getLockTtl());
        return acquired != null && acquired;
    }

    public void release(UUID repositoryId) {
        redisTemplate.delete(LOCK_PREFIX + repositoryId);
    }
}
