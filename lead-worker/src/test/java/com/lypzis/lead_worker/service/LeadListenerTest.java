package com.lypzis.lead_worker.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.lypzis.lead_contracts.dto.LeadDTO;
import com.lypzis.lead_worker.config.RabbitConfig;
import com.rabbitmq.client.Channel;

@ExtendWith(MockitoExtension.class)
class LeadListenerTest {

    @Mock
    private LeadService leadService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private Channel channel;

    @InjectMocks
    private LeadListener leadListener;

    @Test
    void handleLeadShouldProcessAndAckWhenMessageIsNew() throws Exception {
        LeadDTO event = sampleEvent("msg-001");

        when(idempotencyService.alreadyProcessed("tenant-key", "msg-001")).thenReturn(false);

        leadListener.handleLead(event, channel, null, 1L);

        verify(leadService).processLead(event);
        verify(idempotencyService).markProcessed("tenant-key", "msg-001");
        verify(channel).basicAck(1L, false);
    }

    @Test
    void handleLeadShouldIgnoreAlreadyProcessedMessage() throws Exception {
        LeadDTO event = sampleEvent("msg-dup");
        when(idempotencyService.alreadyProcessed("tenant-key", "msg-dup")).thenReturn(true);

        leadListener.handleLead(event, channel, null, 2L);

        verify(channel).basicAck(2L, false);
        verifyNoInteractions(leadService, rabbitTemplate);
        verify(idempotencyService, never()).markProcessed(any(), any());
    }

    @Test
    void handleLeadShouldNackWhenProcessingFailsAndRetryIsBelowThreshold() throws Exception {
        LeadDTO event = sampleEvent("msg-fail");

        when(idempotencyService.alreadyProcessed("tenant-key", "msg-fail")).thenReturn(false);
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(leadService).processLead(event);

        leadListener.handleLead(event, channel, 0L, 9L);

        verify(channel).basicNack(9L, false, false);
        verifyNoInteractions(rabbitTemplate);
        verify(idempotencyService, never()).markProcessed(any(), any());
    }

    @Test
    void handleLeadShouldSendToDlqWhenRetryThresholdIsReached() throws Exception {
        LeadDTO event = sampleEvent("msg-dlq");

        when(idempotencyService.alreadyProcessed("tenant-key", "msg-dlq")).thenReturn(false);
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(leadService).processLead(event);

        leadListener.handleLead(event, channel, 1L, 10L);

        verify(rabbitTemplate).convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.DLQ_ROUTING_KEY,
                event);
        verify(channel, never()).basicNack(10L, false, false);
        verify(channel, never()).basicAck(10L, false);
        verify(idempotencyService, never()).markProcessed(any(), any());
    }

    private LeadDTO sampleEvent(String messageId) {
        LeadDTO event = new LeadDTO();
        event.setMessageId(messageId);
        event.setPhone("+15551234567");
        event.setMessage("hello");
        event.setCampaign("campaign-a");
        event.setTenant("tenant-key");
        return event;
    }
}
