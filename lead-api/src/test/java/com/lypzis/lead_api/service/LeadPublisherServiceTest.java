package com.lypzis.lead_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.lypzis.lead_api.config.RabbitConfig;
import com.lypzis.lead_api.utils.TenantRegistry;
import com.lypzis.lead_contracts.dto.LeadDTO;
import com.lypzis.lead_contracts.dto.LeadEventDTO;

@ExtendWith(MockitoExtension.class)
class LeadPublisherServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private TenantRateLimiterService rateLimiter;

    @Mock
    private TenantRegistry tenantRegistry;

    @InjectMocks
    private LeadPublisherService service;

    @Captor
    private ArgumentCaptor<LeadDTO> leadCaptor;

    @Test
    void shouldThrowTooManyRequestsWhenRateLimitExceeded() {
        when(rateLimiter.allow("tenant-key", 60)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.publish("tenant-key", sampleEvent()));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exception.getReason()).isEqualTo("Rate limit exceeded");
        verifyNoInteractions(tenantRegistry, rabbitTemplate);
    }

    @Test
    void shouldThrowUnauthorizedWhenTenantIsInvalid() {
        when(rateLimiter.allow("tenant-key", 60)).thenReturn(true);
        when(tenantRegistry.isValid("tenant-key")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.publish("tenant-key", sampleEvent()));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void shouldPublishMappedLeadWhenRateLimitAndTenantAreValid() {
        LeadEventDTO event = sampleEvent();
        event.setApiKey("spoofed-body-key");

        when(rateLimiter.allow("tenant-key", 60)).thenReturn(true);
        when(tenantRegistry.isValid("tenant-key")).thenReturn(true);

        service.publish("tenant-key", event);

        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                leadCaptor.capture());

        LeadDTO lead = leadCaptor.getValue();
        assertThat(lead.getTenant()).isEqualTo("tenant-key");
        assertThat(lead.getTenant()).isNotEqualTo(event.getApiKey());
        assertThat(lead.getMessageId()).isEqualTo(event.getMessageId());
        assertThat(lead.getPhone()).isEqualTo(event.getPhone());
        assertThat(lead.getMessage()).isEqualTo(event.getMessage());
        assertThat(lead.getCampaign()).isEqualTo(event.getCampaign());
        assertThat(lead.getVersion()).isEqualTo(1);

        verify(rabbitTemplate).convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY,
                lead);
    }

    private LeadEventDTO sampleEvent() {
        LeadEventDTO event = new LeadEventDTO();
        event.setMessageId("msg-1");
        event.setPhone("+15550001111");
        event.setMessage("hello");
        event.setCampaign("campaign-a");
        return event;
    }
}
