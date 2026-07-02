package com.deveshparmar.codesage.platform.api;

import com.deveshparmar.codesage.platform.config.security.ApiKeyAuthentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityContextHelper {

    private SecurityContextHelper() {
    }

    public static UUID getAuthenticatedOrganizationId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof ApiKeyAuthentication apiKeyAuthentication) {
            return apiKeyAuthentication.getOrganizationId();
        }
        throw new IllegalStateException("No authenticated organization in security context");
    }
}
