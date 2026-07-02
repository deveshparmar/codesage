package com.deveshparmar.codesage.platform.api;

import com.deveshparmar.codesage.platform.api.dto.CreateOrganizationRequest;
import com.deveshparmar.codesage.platform.api.dto.OrganizationResponse;
import com.deveshparmar.codesage.platform.application.OrganizationService;
import com.deveshparmar.codesage.platform.infrastructure.persistence.OrganizationEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        OrganizationEntity entity = organizationService.create(request.name(), request.slug());
        return toResponse(entity);
    }

    @GetMapping
    public List<OrganizationResponse> list() {
        return organizationService.listAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private OrganizationResponse toResponse(OrganizationEntity entity) {
        return new OrganizationResponse(entity.getId(), entity.getName(), entity.getSlug(), entity.getCreatedAt());
    }
}
