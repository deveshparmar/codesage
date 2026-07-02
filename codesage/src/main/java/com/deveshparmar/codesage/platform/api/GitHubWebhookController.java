package com.deveshparmar.codesage.platform.api;

import com.deveshparmar.codesage.platform.application.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/github")
@RequiredArgsConstructor
public class GitHubWebhookController {

    private final WebhookService webhookService;

    @PostMapping("/{organizationId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void handleWebhook(
            @PathVariable UUID organizationId,
            @RequestHeader(name = "X-GitHub-Event") String eventType,
            @RequestHeader(name = "X-GitHub-Delivery") String deliveryId,
            @RequestHeader(name = "X-Hub-Signature-256") String signature,
            @RequestBody String payload,
            HttpServletRequest request
    ) {
        webhookService.handleGitHubWebhook(
                organizationId,
                eventType,
                deliveryId,
                signature,
                payload,
                request.getRemoteAddr()
        );
    }
}
