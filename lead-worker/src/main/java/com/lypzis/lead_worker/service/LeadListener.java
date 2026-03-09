package com.lypzis.lead_worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.lypzis.lead_worker.config.RabbitConfig;
import com.lypzis.lead_worker.dto.LeadEventDTO;
import com.lypzis.lead_worker.entity.Lead;
import com.lypzis.lead_worker.repository.LeadRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadListener {

    private final LeadRepository leadRepository;
    private final AutomationRuleService ruleService;
    private final WhatsAppSender sender;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void handleLead(LeadEventDTO event) {

        log.info("Received lead event {}", event.getMessageId());

        if (leadRepository.findByMessageId(event.getMessageId()).isPresent()) {
            log.info("Duplicate message ignored: {}", event.getMessageId());
            return;
        }

        Lead lead = Lead.builder()
                .messageId(event.getMessageId())
                .phone(event.getPhone())
                .message(event.getMessage())
                .campaign(event.getCampaign())
                .build();

        leadRepository.save(lead);

        log.info("Lead stored successfully {}", lead.getId());

        ruleService.matchRule(event.getMessage())
                .ifPresent(rule -> sender.sendMessage(
                        event.getPhone(),
                        rule.getResponseMessage()));
    }
}
