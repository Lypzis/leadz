package com.lypzis.lead_worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;

import java.io.IOException;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.messaging.handler.annotation.Header;

import com.lypzis.lead_worker.config.RabbitConfig;
import com.lypzis.lead_worker.entity.Lead;
import com.lypzis.lead_worker.entity.Tenant;
import com.lypzis.lead_worker.repository.LeadRepository;
import com.lypzis.lead_worker.repository.TenantRepository;
import com.lypzis.lead_contracts.dto.LeadDTO;
import com.rabbitmq.client.Channel;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadListener {

    private static final long MAX_RETRIES_BEFORE_DLQ = 1;

    private final LeadRepository leadRepository;
    private final TenantRepository tenantRepository;
    private final AutomationRuleService ruleService;
    private final WhatsAppSender sender;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitConfig.MAIN_QUEUE)
    public void handleLead(LeadDTO event, Channel channel,
            @Header(name = AmqpHeaders.RETRY_COUNT, required = false) Long retryCount,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {

            processLead(event);

            channel.basicAck(tag, false);

        } catch (Exception e) {
            if (retryCount != null && retryCount >= MAX_RETRIES_BEFORE_DLQ) {
                log.error("Sending message {} to DLQ after {} retries", event.getMessageId(), retryCount, e);
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.DLQ_ROUTING_KEY, event);
                return;
            }

            log.error("Failed processing lead {}", event.getMessageId(), e);

            throw e; // triggers retry mechanism

        }
    }

    private void processLead(LeadDTO event) {
        if (event.getVersion() != 1) {
            log.warn("Unsupported event version {}", event.getVersion());
            return;
        }

        Tenant tenant = tenantRepository
                .findByApiKey(event.getTenant())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

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
                .tenant(tenant)
                .build();

        leadRepository.save(lead);

        log.info("Lead stored successfully {}", lead.getId());

        ruleService.matchRule(event.getMessage())
                .ifPresent(rule -> sender.sendMessage(
                        event.getPhone(),
                        rule.getResponseMessage()));
    }
}
