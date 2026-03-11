package com.lypzis.lead_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lypzis.lead_contracts.dto.CreateTenantRequestDTO;
import com.lypzis.lead_contracts.dto.TenantResponseDTO;
import com.lypzis.lead_api.exception.ResourceNotFoundException;
import com.lypzis.lead_api.exception.UnauthorizedException;
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
    private final TenantService tenantService;

    @Transactional
    public TenantResponseDTO create(CreateTenantRequestDTO request) {
        if (request.whatsappPhoneNumberId() == null || request.whatsappPhoneNumberId().isBlank()) {
            throw new IllegalArgumentException("whatsappPhoneNumberId is required");
        }
        tenantRepository.findByWhatsappPhoneNumberId(request.whatsappPhoneNumberId())
                .ifPresent(tenant -> {
                    throw new IllegalArgumentException("whatsappPhoneNumberId already in use");
                });

        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setPlan(request.plan());
        tenant.setRequestsPerMinute(request.requestsPerMinute());
        tenant.setWhatsappPhoneNumberId(request.whatsappPhoneNumberId());
        tenant.setActive(true);
        tenant.setApiKey(apiKeyGenerator.generate());

        Tenant saved = tenantRepository.save(tenant);
        adminUserProvisioningService.createInitialAdmin(saved, request.adminEmail(), request.adminPassword());
        return toResponse(saved);
    }

    public TenantResponseDTO getById(Long id) {
        Tenant tenant = getOwnedTenantOrThrow(id);
        return toResponse(tenant);
    }

    public List<TenantResponseDTO> list() {
        Tenant tenant = tenantService.getCurrentTenant();
        return List.of(toResponse(tenant));
    }

    public void deactivate(Long id) {
        Tenant tenant = getOwnedTenantOrThrow(id);
        tenant.setActive(false);
        tenantRepository.save(tenant);
    }

    public TenantResponseDTO regenerateApiKey(Long id) {
        Tenant tenant = getOwnedTenantOrThrow(id);
        tenant.setApiKey(apiKeyGenerator.generate());
        Tenant saved = tenantRepository.save(tenant);
        return toResponse(saved);
    }

    private Tenant getOwnedTenantOrThrow(Long requestedTenantId) {
        Tenant currentTenant = tenantService.getCurrentTenant();

        if (requestedTenantId == null || !requestedTenantId.equals(currentTenant.getId())) {
            throw new UnauthorizedException("Tenant access denied");
        }
        return tenantRepository.findById(currentTenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + currentTenant.getId()));
    }

    private TenantResponseDTO toResponse(Tenant tenant) {
        return new TenantResponseDTO(
                tenant.getId(),
                tenant.getName(),
                tenant.getApiKey(),
                tenant.getWhatsappPhoneNumberId(),
                tenant.getPlan(),
                tenant.getRequestsPerMinute(),
                tenant.getActive(),
                tenant.getCreatedAt());
    }
}
