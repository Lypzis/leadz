package com.lypzis.lead_api.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.lypzis.lead_api.exception.ResourceNotFoundException;
import com.lypzis.lead_contracts.dto.LeadStatusEnum;
import com.lypzis.lead_domain.entity.Lead;
import com.lypzis.lead_domain.entity.LeadMessage;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_domain.repository.LeadMessageRepository;
import com.lypzis.lead_domain.repository.LeadRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeadQueryService {

    private final LeadRepository leadRepository;
    private final LeadMessageRepository messageRepository;
    private final TenantService tenantService;

    public Page<Lead> listLeads(LeadStatusEnum status, Pageable pageable) {
        Tenant tenant = tenantService.getCurrentTenant();
        String tenantId = tenant.getApiKey();

        if (status != null) {
            return leadRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        }

        return leadRepository.findByTenantId(tenantId, pageable);
    }

    public Lead getById(Long id) {
        Tenant tenant = tenantService.getCurrentTenant();

        return leadRepository.findByIdAndTenantId(id, tenant.getApiKey())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
    }

    public List<LeadMessage> getTimeline(Long leadId) {
        Lead lead = getById(leadId);
        return messageRepository.findByLeadIdOrderByCreatedAtAsc(lead.getId());
    }
}
