package com.deveshparmar.codesage.scm.domain;

import com.deveshparmar.codesage.common.domain.ScmProviderType;

import java.util.Map;

public record WebhookEvent(
        ScmProviderType provider,
        String eventType,
        String action,
        String deliveryId,
        Map<String, Object> payload
) {
}
