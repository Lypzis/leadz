package com.lypzis.lead_worker.service;

import org.springframework.stereotype.Service;

import com.lypzis.lead_contracts.dto.LeadDTO;
import com.lypzis.lead_domain.entity.Lead;
import com.lypzis.lead_domain.entity.LeadStatus;
import com.lypzis.lead_domain.repository.LeadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService {

    private final LeadRepository leadRepository;
    private final AutomationRuleService ruleService;
    private final LeadMessageService leadMessageService;
    private final AutomationExecutor automationExecutor;

    public Lead findOrCreateLead(String tenant, String phone, String campaign) {
        return leadRepository.findByTenantAndPhone(tenant, phone)
                .orElseGet(() -> leadRepository.save(
                        Lead.builder()
                                .tenant(tenant)
                                .phone(phone)
                                .status(LeadStatus.NEW)
                                .campaign(campaign)
                                .build()));
    }

    public void updateStatus(Lead lead, LeadStatus status) {
        lead.setStatus(status);
        leadRepository.save(lead);
    }

    public void processLead(LeadDTO event) {

        if (event.getVersion() != 1) {
            log.warn("Unsupported event version {}", event.getVersion());
            return;
        }

        log.info("Received lead event {}", event.getMessageId());

        Lead lead = findOrCreateLead(
                event.getTenant(),
                event.getPhone(),
                event.getCampaign());

        leadMessageService.saveInbound(
                lead,
                event.getTenant(),
                event.getMessageId(),
                event.getMessage());

        ruleService.matchRule(event.getTenant(), event.getMessage())
                .ifPresent(rule -> {

                    automationExecutor.execute(
                            rule,
                            lead,
                            event.getPhone());

                });

    }
}
