package com.deveshparmar.codesage.platform.api;

import com.deveshparmar.codesage.platform.api.dto.ApiKeyResponse;
import com.deveshparmar.codesage.platform.api.dto.CreateApiKeyRequest;
import com.deveshparmar.codesage.platform.application.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyResponse create(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateApiKeyRequest request
    ) {
        ApiKeyService.ApiKeyCreationResult result = apiKeyService.createApiKey(organizationId, request.name());
        return new ApiKeyResponse(
                result.apiKeyId(),
                result.rawKey(),
                result.prefix(),
                "Store this API key securely. It will not be shown again."
        );
    }
}
