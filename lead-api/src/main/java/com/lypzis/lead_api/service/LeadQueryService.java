package com.lypzis.lead_api.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.lypzis.lead_domain.entity.Lead;
import com.lypzis.lead_domain.entity.LeadMessage;
import com.lypzis.lead_domain.entity.LeadStatus;
import com.lypzis.lead_domain.repository.LeadMessageRepository;
import com.lypzis.lead_domain.repository.LeadRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeadQueryService {

    private final LeadRepository leadRepository;
    private final LeadMessageRepository messageRepository;

    public Page<Lead> listLeads(String tenant, LeadStatus status, Pageable pageable) {

        if (status != null) {
            return leadRepository.findByTenantAndStatus(tenant, status, pageable);
        }

        return leadRepository.findByTenant(tenant, pageable);
    }

    public List<LeadMessage> getTimeline(Long leadId) {
        return messageRepository.findByLeadIdOrderByCreatedAtAsc(leadId);
    }
}
