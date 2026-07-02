package com.deveshparmar.codesage.platform.application;

import com.deveshparmar.codesage.common.exception.DuplicateResourceException;
import com.deveshparmar.codesage.common.exception.ResourceNotFoundException;
import com.deveshparmar.codesage.platform.infrastructure.persistence.OrganizationEntity;
import com.deveshparmar.codesage.platform.infrastructure.persistence.OrganizationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationJpaRepository organizationRepository;

    @Transactional(readOnly = true)
    public OrganizationEntity getById(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId.toString()));
    }

    @Transactional(readOnly = true)
    public List<OrganizationEntity> listAll() {
        return organizationRepository.findAll();
    }

    @Transactional
    public OrganizationEntity create(String name, String slug) {
        if (organizationRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Organization slug already exists: " + slug);
        }
        OrganizationEntity entity = new OrganizationEntity();
        entity.setName(name);
        entity.setSlug(slug);
        return organizationRepository.save(entity);
    }
}
