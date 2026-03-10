package com.lypzis.lead_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.lypzis.lead_api.config.RabbitConfig;
import com.lypzis.lead_api.exception.RateLimitExceededException;
import com.lypzis.lead_api.exception.UnauthorizedException;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_contracts.dto.LeadDTO;
import com.lypzis.lead_contracts.dto.LeadEventDTO;
import com.lypzis.lead_contracts.dto.TenantPlanEnum;

@ExtendWith(MockitoExtension.class)
class LeadPublisherServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private TenantRateLimiterService rateLimiter;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private LeadPublisherService service;

    @Captor
    private ArgumentCaptor<LeadDTO> leadCaptor;

    @Test
    void shouldThrowTooManyRequestsWhenRateLimitExceeded() {
        Tenant tenant = activeTenant("tenant-key", 60);
        when(tenantService.resolveTenant("tenant-key")).thenReturn(tenant);
        when(rateLimiter.allow("tenant-key", 60)).thenReturn(false);

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> service.publish("tenant-key", sampleEvent()));

        assertThat(exception.getMessage()).isEqualTo("Rate limit exceeded");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void shouldThrowUnauthorizedWhenTenantCannotBeResolved() {
        when(tenantService.resolveTenant("tenant-key"))
                .thenThrow(new UnauthorizedException("Invalid or inactive API key"));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> service.publish("tenant-key", sampleEvent()));

        assertThat(exception.getMessage()).isEqualTo("Invalid or inactive API key");
        verifyNoInteractions(rateLimiter, rabbitTemplate);
    }

    @Test
    void shouldPublishMappedLeadWhenRateLimitAndTenantAreValid() {
        Tenant tenant = activeTenant("tenant-key", 75);
        LeadEventDTO event = sampleEvent();
        event.setApiKey("spoofed-body-key");

        when(tenantService.resolveTenant("tenant-key")).thenReturn(tenant);
        when(rateLimiter.allow("tenant-key", 75)).thenReturn(true);

        service.publish("tenant-key", event);

        verify(rateLimiter).allow("tenant-key", 75);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE),
                eq(RabbitConfig.ROUTING_KEY),
                leadCaptor.capture());

        LeadDTO lead = leadCaptor.getValue();
        assertThat(lead.getTenant()).isEqualTo("tenant-key");
        assertThat(lead.getTenant()).isNotEqualTo(event.getApiKey());
        assertThat(lead.getMessageId()).isEqualTo(event.getMessageId());
        assertThat(lead.getPhone()).isEqualTo(event.getPhone());
        assertThat(lead.getMessage()).isEqualTo(event.getMessage());
        assertThat(lead.getCampaign()).isEqualTo(event.getCampaign());
        assertThat(lead.getVersion()).isEqualTo(1);
    }

    private LeadEventDTO sampleEvent() {
        LeadEventDTO event = new LeadEventDTO();
        event.setMessageId("msg-1");
        event.setPhone("+15550001111");
        event.setMessage("hello");
        event.setCampaign("campaign-a");
        return event;
    }

    private Tenant activeTenant(String apiKey, int requestsPerMinute) {
        Tenant tenant = new Tenant();
        tenant.setName("Tenant A");
        tenant.setApiKey(apiKey);
        tenant.setPlan(TenantPlanEnum.PRO);
        tenant.setRequestsPerMinute(requestsPerMinute);
        tenant.setActive(true);
        return tenant;
    }
}
