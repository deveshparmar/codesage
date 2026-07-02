package com.deveshparmar.codesage.platform.config.security;

import com.deveshparmar.codesage.platform.application.ApiKeyService;
import com.deveshparmar.codesage.platform.config.CodeSageProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final CodeSageProperties codeSageProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String apiKey = request.getHeader(codeSageProperties.getSecurity().getApiKeyHeader());
            if (apiKey != null && !apiKey.isBlank()) {
                var validatedKey = apiKeyService.validateApiKey(apiKey);
                SecurityContextHolder.getContext().setAuthentication(new ApiKeyAuthentication(validatedKey));
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/api/v1/webhooks");
    }
}
