package com.deveshparmar.codesage.scm.domain;

public interface WebhookVerifier {

    void verify(String payload, String signature, String webhookSecret);

    WebhookEvent parseEvent(String eventType, String deliveryId, String payload);
}
