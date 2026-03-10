package com.lypzis.lead_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lypzis.lead_api.dto.AutomationRuleRequestDTO;
import com.lypzis.lead_api.dto.AutomationRuleResponseDTO;
import com.lypzis.lead_api.exception.ResourceNotFoundException;
import com.lypzis.lead_domain.entity.AutomationRule;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_domain.repository.AutomationRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutomationRuleService {

    private final AutomationRuleRepository automationRuleRepository;
    private final TenantService tenantService;

    public AutomationRuleResponseDTO create(AutomationRuleRequestDTO request) {
        validateRequest(request);
        Tenant tenant = tenantService.getCurrentTenant();

        AutomationRule rule = new AutomationRule();
        rule.setTenantId(tenant.getApiKey());
        rule.setKeyword(request.keyword().trim());
        rule.setPriority(request.priority());
        rule.setActionType(request.actionType());
        rule.setActionPayload(normalizePayload(request.actionPayload()));

        AutomationRule saved = automationRuleRepository.save(rule);
        return toResponse(saved);
    }

    public List<AutomationRuleResponseDTO> list() {
        Tenant tenant = tenantService.getCurrentTenant();
        return automationRuleRepository.findByTenantIdOrderByPriorityDesc(tenant.getApiKey())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AutomationRuleResponseDTO update(Long id, AutomationRuleRequestDTO request) {
        validateRequest(request);
        Tenant tenant = tenantService.getCurrentTenant();

        AutomationRule rule = automationRuleRepository.findByIdAndTenantId(id, tenant.getApiKey())
                .orElseThrow(() -> new ResourceNotFoundException("Automation rule not found: " + id));

        rule.setKeyword(request.keyword().trim());
        rule.setPriority(request.priority());
        rule.setActionType(request.actionType());
        rule.setActionPayload(normalizePayload(request.actionPayload()));

        AutomationRule saved = automationRuleRepository.save(rule);
        return toResponse(saved);
    }

    public void delete(Long id) {
        Tenant tenant = tenantService.getCurrentTenant();

        AutomationRule rule = automationRuleRepository.findByIdAndTenantId(id, tenant.getApiKey())
                .orElseThrow(() -> new ResourceNotFoundException("Automation rule not found: " + id));

        automationRuleRepository.delete(rule);
    }

    private AutomationRuleResponseDTO toResponse(AutomationRule rule) {
        return new AutomationRuleResponseDTO(
                rule.getId(),
                rule.getTenantId(),
                rule.getKeyword(),
                rule.getPriority(),
                rule.getActionType(),
                rule.getActionPayload(),
                rule.getCreatedAt());
    }

    private void validateRequest(AutomationRuleRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.keyword() == null || request.keyword().isBlank()) {
            throw new IllegalArgumentException("keyword is required");
        }
        if (request.priority() == null || request.priority() < 0) {
            throw new IllegalArgumentException("priority must be >= 0");
        }
        if (request.actionType() == null) {
            throw new IllegalArgumentException("actionType is required");
        }
        if (request.actionPayload() != null && request.actionPayload().length() > 2000) {
            throw new IllegalArgumentException("actionPayload must be <= 2000 chars");
        }
    }

    private String normalizePayload(String payload) {
        if (payload == null) {
            return null;
        }
        String trimmed = payload.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
