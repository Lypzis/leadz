package com.lypzis.lead_api.service;

import com.lypzis.lead_api.config.RabbitConfig;
import com.lypzis.lead_api.exception.RateLimitExceededException;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_contracts.dto.LeadDTO;
import com.lypzis.lead_contracts.dto.LeadEventDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LeadPublisherService {

    private final RabbitTemplate rabbitTemplate;
    private final TenantRateLimiterService rateLimiter;
    private final TenantService tenantService;

    public void publish(String apiKey, LeadEventDTO event) {

        Tenant tenant = tenantService.resolveTenant(apiKey);

        if (!rateLimiter.allow(apiKey, tenant.getRequestsPerMinute())) {
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
    }
}
