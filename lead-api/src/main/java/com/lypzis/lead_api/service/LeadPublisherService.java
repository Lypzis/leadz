package com.lypzis.lead_api.service;

import com.lypzis.lead_api.config.RabbitConfig;
import com.lypzis.lead_api.exception.RateLimitExceededException;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_contracts.dto.LeadDTO;
import com.lypzis.lead_contracts.dto.LeadEventDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadPublisherService {

    private final RabbitTemplate rabbitTemplate;
    private final TenantRateLimiterService rateLimiter;

    public void publish(LeadEventDTO event, Tenant tenant) {
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant is required for publishing");
        }

        if (!rateLimiter.allow(tenant.getApiKey(), tenant.getRequestsPerMinute())) {
            log.warn("Rate limit exceeded for tenantId={} apiKey={}", tenant.getId(), tenant.getApiKey());
            throw new RateLimitExceededException("Rate limit exceeded");
        }

        LeadDTO lead = new LeadDTO();

        lead.setTenant(tenant.getApiKey());
        lead.setMessageId(event.getMessageId());
        lead.setPhone(event.getPhone());
        lead.setMessage(event.getMessage());
        lead.setCampaign(event.getCampaign());

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY,
                lead);
        log.info("Published lead messageId={} tenant={} phone={} exchange={} routingKey={}",
                event.getMessageId(),
                tenant.getApiKey(),
                event.getPhone(),
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY);
    }
}
