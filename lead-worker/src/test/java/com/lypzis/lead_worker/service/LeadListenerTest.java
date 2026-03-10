package com.lypzis.lead_worker.service;

import com.lypzis.lead_contracts.dto.LeadDTO;
import com.lypzis.lead_worker.entity.Lead;
import com.lypzis.lead_worker.config.RabbitConfig;
import com.lypzis.lead_worker.repository.LeadRepository;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadListenerTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private AutomationRuleService ruleService;

    @Mock
    private WhatsAppSender sender;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private Channel channel;

    @InjectMocks
    private LeadListener leadListener;

    @Captor
    private ArgumentCaptor<Lead> leadCaptor;

    @Test
    void handleLeadShouldPersistWhenMessageIsNew() throws Exception {
        LeadDTO event = sampleEvent("msg-001");

        when(idempotencyService.alreadyProcessed("tenant-key", "msg-001")).thenReturn(false);
        when(leadRepository.findByTenantAndMessageId("tenant-key", "msg-001")).thenReturn(Optional.empty());
        when(ruleService.matchRule("tenant-key", "hello")).thenReturn(Optional.empty());

        leadListener.handleLead(event, channel, null, 1L);

        verify(leadRepository).save(leadCaptor.capture());
        Lead savedLead = leadCaptor.getValue();
        assertThat(savedLead.getMessageId()).isEqualTo("msg-001");
        assertThat(savedLead.getPhone()).isEqualTo("+15551234567");
        assertThat(savedLead.getMessage()).isEqualTo("hello");
        assertThat(savedLead.getCampaign()).isEqualTo("campaign-a");
        assertThat(savedLead.getTenant()).isEqualTo("tenant-key");
        verify(idempotencyService).markProcessed("tenant-key", "msg-001");
        verify(channel).basicAck(1L, false);
    }

    @Test
    void handleLeadShouldIgnoreAlreadyProcessedMessage() throws Exception {
        LeadDTO event = sampleEvent("msg-dup");
        when(idempotencyService.alreadyProcessed("tenant-key", "msg-dup")).thenReturn(true);

        leadListener.handleLead(event, channel, null, 2L);

        verify(channel).basicAck(2L, false);
        verifyNoInteractions(leadRepository, ruleService, sender, rabbitTemplate);
        verify(idempotencyService, never()).markProcessed(any(), any());
    }

    @Test
    void handleLeadShouldAcknowledgeDuplicateFromLeadRepository() throws Exception {
        LeadDTO event = sampleEvent("msg-db-dup");
        Lead existingLead = Lead.builder().messageId("msg-db-dup").tenant("tenant-key").build();

        when(idempotencyService.alreadyProcessed("tenant-key", "msg-db-dup")).thenReturn(false);
        when(leadRepository.findByTenantAndMessageId("tenant-key", "msg-db-dup")).thenReturn(Optional.of(existingLead));

        leadListener.handleLead(event, channel, null, 3L);

        verify(leadRepository, never()).save(any(Lead.class));
        verify(idempotencyService).markProcessed("tenant-key", "msg-db-dup");
        verify(channel).basicAck(3L, false);
    }

    @Test
    void handleLeadShouldNackWhenProcessingFailsAndRetryIsBelowThreshold() throws Exception {
        LeadDTO event = sampleEvent("msg-fail");

        when(idempotencyService.alreadyProcessed("tenant-key", "msg-fail")).thenReturn(false);
        when(leadRepository.findByTenantAndMessageId("tenant-key", "msg-fail"))
                .thenThrow(new RuntimeException("db down"));

        leadListener.handleLead(event, channel, 0L, 9L);

        verify(channel).basicNack(9L, false, false);
        verifyNoInteractions(rabbitTemplate);
        verify(idempotencyService, never()).markProcessed(any(), any());
    }

    @Test
    void handleLeadShouldSendToDlqWhenRetryThresholdIsReached() throws Exception {
        LeadDTO event = sampleEvent("msg-dlq");

        when(idempotencyService.alreadyProcessed("tenant-key", "msg-dlq")).thenReturn(false);
        when(leadRepository.findByTenantAndMessageId("tenant-key", "msg-dlq"))
                .thenThrow(new RuntimeException("db down"));

        leadListener.handleLead(event, channel, 1L, 10L);

        verify(rabbitTemplate).convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.DLQ_ROUTING_KEY,
                event);
        verify(channel, never()).basicNack(10L, false, false);
        verify(channel, never()).basicAck(10L, false);
        verify(idempotencyService, never()).markProcessed(any(), any());
    }

    @Test
    void handleLeadShouldNotTreatSameMessageIdFromAnotherTenantAsDuplicate() throws Exception {
        LeadDTO event = sampleEvent("msg-shared");
        event.setTenant("tenant-b");

        when(idempotencyService.alreadyProcessed("tenant-b", "msg-shared")).thenReturn(false);
        when(leadRepository.findByTenantAndMessageId("tenant-b", "msg-shared")).thenReturn(Optional.empty());
        when(ruleService.matchRule("tenant-b", "hello")).thenReturn(Optional.empty());

        leadListener.handleLead(event, channel, null, 11L);

        verify(leadRepository).save(leadCaptor.capture());
        Lead savedLead = leadCaptor.getValue();
        assertThat(savedLead.getMessageId()).isEqualTo("msg-shared");
        assertThat(savedLead.getTenant()).isEqualTo("tenant-b");
        verify(idempotencyService).markProcessed("tenant-b", "msg-shared");
        verify(channel).basicAck(11L, false);
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
