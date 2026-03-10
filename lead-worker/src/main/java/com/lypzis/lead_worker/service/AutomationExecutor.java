package com.lypzis.lead_worker.service;

import org.springframework.stereotype.Service;

import com.lypzis.lead_contracts.dto.LeadStatusEnum;
import com.lypzis.lead_domain.entity.AutomationRule;
import com.lypzis.lead_domain.entity.Lead;
import com.lypzis.lead_domain.repository.LeadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationExecutor {

    private final WhatsAppSender sender;
    private final LeadMessageService leadMessageService;
    private final LeadRepository leadRepository;

    private LeadStatusEnum parseStatus(String value) {
        try {
            return LeadStatusEnum.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid lead status: " + value, e);
        }
    }

    public void execute(AutomationRule rule, Lead lead, String phone) {

        if (rule == null || lead == null) {
            log.warn("Skipping automation execution because rule or lead is null");
            return;
        }

        if (rule.getActionType() == null) {
            log.warn("Skipping automation execution because actionType is null for tenant {}", rule.getTenantId());
            return;
        }

        switch (rule.getActionType()) {

            case SEND_MESSAGE -> {

                if (isBlank(rule.getActionPayload()) || isBlank(phone)) {
                    log.warn("Skipping SEND_MESSAGE action due to blank payload or phone for tenant {}",
                            rule.getTenantId());
                    return;
                }

                sender.sendMessage(phone, rule.getActionPayload());

                leadMessageService.saveOutbound(
                        lead,
                        lead.getTenantId(),
                        rule.getActionPayload());

                lead.setStatus(LeadStatusEnum.WAITING_CUSTOMER);
                leadRepository.save(lead);
            }

            case CHANGE_STATUS -> {

                if (isBlank(rule.getActionPayload())) {
                    log.warn("Skipping CHANGE_STATUS action due to blank payload for tenant {}", rule.getTenantId());
                    return;
                }

                lead.setStatus(parseStatus(rule.getActionPayload()));
                leadRepository.save(lead);
            }

            case TAG_LEAD -> {
                // TODO future feature
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
