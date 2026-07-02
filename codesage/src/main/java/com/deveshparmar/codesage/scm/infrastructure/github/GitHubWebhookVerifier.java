package com.deveshparmar.codesage.scm.infrastructure.github;

import com.deveshparmar.codesage.common.domain.ScmProviderType;
import com.deveshparmar.codesage.common.exception.UnauthorizedException;
import com.deveshparmar.codesage.scm.domain.WebhookEvent;
import com.deveshparmar.codesage.scm.domain.WebhookVerifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

class GitHubWebhookVerifier implements WebhookVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final ObjectMapper objectMapper;

    GitHubWebhookVerifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void verify(String payload, String signature, String webhookSecret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new UnauthorizedException("Webhook secret is not configured");
        }
        if (signature == null || signature.isBlank()) {
            throw new UnauthorizedException("Missing webhook signature");
        }
        String expected = "sha256=" + computeHmacSha256(payload, webhookSecret);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new UnauthorizedException("Invalid webhook signature");
        }
    }

    @Override
    public WebhookEvent parseEvent(String eventType, String deliveryId, String payload) {
        try {
            Map<String, Object> payloadMap = objectMapper.readValue(payload, new TypeReference<>() {});
            Object actionValue = payloadMap.get("action");
            String action = actionValue instanceof String actionString ? actionString : null;
            return new WebhookEvent(ScmProviderType.GITHUB, eventType, action, deliveryId, payloadMap);
        } catch (Exception ex) {
            throw new UnauthorizedException("Failed to parse webhook payload");
        }
    }

    private String computeHmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new UnauthorizedException("Failed to compute webhook signature");
        }
    }
}
