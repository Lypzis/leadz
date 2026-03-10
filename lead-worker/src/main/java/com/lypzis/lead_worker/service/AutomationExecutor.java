package com.lypzis.lead_worker.service;

import org.springframework.stereotype.Service;

import com.lypzis.lead_worker.entity.AutomationRule;
import com.lypzis.lead_worker.entity.Lead;
import com.lypzis.lead_worker.repository.LeadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationExecutor {

    private final MessageSender sender;
    private final LeadMessageService leadMessageService;
    private final LeadRepository leadRepository;

    public void execute(AutomationRule rule, Lead lead, String phone) {

        if (rule == null || lead == null) {
            log.warn("Skipping automation execution because rule or lead is null");
            return;
        }

        if (rule.getActionType() == null) {
            log.warn("Skipping automation execution because actionType is null for tenant {}", rule.getTenant());
            return;
        }

        switch (rule.getActionType()) {

            case SEND_MESSAGE -> {

                if (isBlank(rule.getActionPayload()) || isBlank(phone)) {
                    log.warn("Skipping SEND_MESSAGE action due to blank payload or phone for tenant {}", rule.getTenant());
                    return;
                }

                sender.sendMessage(phone, rule.getActionPayload());

                leadMessageService.saveOutbound(
                        lead,
                        lead.getTenant(),
                        rule.getActionPayload());
            }

            case CHANGE_STATUS -> {

                if (isBlank(rule.getActionPayload())) {
                    log.warn("Skipping CHANGE_STATUS action due to blank payload for tenant {}", rule.getTenant());
                    return;
                }

                lead.setStatus(rule.getActionPayload());
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
