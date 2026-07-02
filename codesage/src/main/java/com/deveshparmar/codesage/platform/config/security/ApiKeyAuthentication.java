package com.deveshparmar.codesage.platform.config.security;

import com.deveshparmar.codesage.platform.infrastructure.persistence.ApiKeyEntity;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

@Getter
public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final ApiKeyEntity apiKey;
    private final UUID organizationId;

    public ApiKeyAuthentication(ApiKeyEntity apiKey) {
        super(List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
        this.apiKey = apiKey;
        this.organizationId = apiKey.getOrganizationId();
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey.getKeyHash();
    }

    @Override
    public Object getPrincipal() {
        return organizationId;
    }
}
