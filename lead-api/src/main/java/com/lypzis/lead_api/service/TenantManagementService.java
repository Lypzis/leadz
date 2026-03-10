package com.lypzis.lead_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lypzis.lead_contracts.dto.CreateTenantRequestDTO;
import com.lypzis.lead_contracts.dto.TenantResponseDTO;
import com.lypzis.lead_api.exception.ResourceNotFoundException;
import com.lypzis.lead_api.security.ApiKeyGenerator;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_domain.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantManagementService {

    private final TenantRepository tenantRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final AdminUserProvisioningService adminUserProvisioningService;

    @Transactional
    public TenantResponseDTO create(CreateTenantRequestDTO request) {
        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setPlan(request.plan());
        tenant.setRequestsPerMinute(request.requestsPerMinute());
        tenant.setActive(true);
        tenant.setApiKey(apiKeyGenerator.generate());

        Tenant saved = tenantRepository.save(tenant);
        adminUserProvisioningService.createInitialAdmin(saved, request.adminEmail(), request.adminPassword());
        return toResponse(saved);
    }

    public TenantResponseDTO getById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
        return toResponse(tenant);
    }

    public List<TenantResponseDTO> list() {
        return tenantRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public void deactivate(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
        tenant.setActive(false);
        tenantRepository.save(tenant);
    }

    public TenantResponseDTO regenerateApiKey(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
        tenant.setApiKey(apiKeyGenerator.generate());
        Tenant saved = tenantRepository.save(tenant);
        return toResponse(saved);
    }

    private TenantResponseDTO toResponse(Tenant tenant) {
        return new TenantResponseDTO(
                tenant.getId(),
                tenant.getName(),
                tenant.getApiKey(),
                tenant.getPlan(),
                tenant.getRequestsPerMinute(),
                tenant.getActive(),
                tenant.getCreatedAt());
    }
}
