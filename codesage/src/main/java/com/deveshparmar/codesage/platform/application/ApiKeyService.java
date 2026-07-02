package com.deveshparmar.codesage.platform.application;

import com.deveshparmar.codesage.common.exception.UnauthorizedException;
import com.deveshparmar.codesage.platform.infrastructure.persistence.ApiKeyEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.ApiKeyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyJpaRepository apiKeyRepository;
    private final OrganizationService organizationService;

    @Transactional
    public ApiKeyCreationResult createApiKey(UUID organizationId, String name) {
        organizationService.getById(organizationId);
        String rawKey = generateRawKey();
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setOrganizationId(organizationId);
        entity.setName(name);
        entity.setPrefix(rawKey.substring(0, 8));
        entity.setKeyHash(hashKey(rawKey));
        entity.setActive(true);
        apiKeyRepository.save(entity);
        return new ApiKeyCreationResult(entity.getId(), rawKey, entity.getPrefix());
    }

    @Transactional(readOnly = true)
    public ApiKeyEntity validateApiKey(String rawKey) {
        ApiKeyEntity apiKey = apiKeyRepository.findByKeyHashAndActiveTrue(hashKey(rawKey))
                .orElseThrow(() -> new UnauthorizedException("Invalid API key"));
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("API key has expired");
        }
        return apiKey;
    }

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "cs_" + HexFormat.of().formatHex(bytes);
    }

    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public record ApiKeyCreationResult(UUID apiKeyId, String rawKey, String prefix) {
    }
}
