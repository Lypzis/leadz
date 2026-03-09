package com.lypzis.lead_worker.service;

import com.lypzis.lead_worker.dto.LeadEventDTO;
import com.lypzis.lead_worker.entity.Lead;
import com.lypzis.lead_worker.entity.Tenant;
import com.lypzis.lead_worker.repository.LeadRepository;
import com.lypzis.lead_worker.repository.TenantRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadListenerTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AutomationRuleService ruleService;

    @Mock
    private WhatsAppSender sender;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private LeadListener leadListener;

    @Captor
    private ArgumentCaptor<Lead> leadCaptor;

    @Test
    void handleLeadShouldPersistWhenMessageIsNew() {
        LeadEventDTO event = new LeadEventDTO();
        event.setMessageId("msg-001");
        event.setPhone("+15551234567");
        event.setMessage("hello");
        event.setCampaign("campaign-a");
        event.setApiKey("tenant-key");

        Tenant tenant = Tenant.builder()
                .name("Tenant A")
                .apiKey("tenant-key")
                .build();
        tenant.setId(10L);

        when(tenantRepository.findByApiKey("tenant-key")).thenReturn(Optional.of(tenant));
        when(leadRepository.findByMessageId("msg-001")).thenReturn(Optional.empty());
        when(ruleService.matchRule("hello")).thenReturn(Optional.empty());

        leadListener.handleLead(event, 0L);

        verify(leadRepository).save(leadCaptor.capture());
        Lead savedLead = leadCaptor.getValue();
        assertThat(savedLead.getMessageId()).isEqualTo("msg-001");
        assertThat(savedLead.getPhone()).isEqualTo("+15551234567");
        assertThat(savedLead.getMessage()).isEqualTo("hello");
        assertThat(savedLead.getCampaign()).isEqualTo("campaign-a");
        assertThat(savedLead.getTenant()).isNotNull();
        assertThat(savedLead.getTenant().getId()).isEqualTo(10L);
        assertThat(savedLead.getTenant().getApiKey()).isEqualTo("tenant-key");
    }

    @Test
    void handleLeadShouldIgnoreDuplicateMessage() {
        LeadEventDTO event = new LeadEventDTO();
        event.setMessageId("msg-dup");
        event.setApiKey("tenant-key");

        Tenant tenant = Tenant.builder()
                .name("Tenant A")
                .apiKey("tenant-key")
                .build();
        tenant.setId(10L);

        Lead existingLead = Lead.builder().messageId("msg-dup").build();
        existingLead.setId(1L);
        when(tenantRepository.findByApiKey("tenant-key")).thenReturn(Optional.of(tenant));
        when(leadRepository.findByMessageId("msg-dup"))
                .thenReturn(Optional.of(existingLead));

        leadListener.handleLead(event, 0L);

        verify(leadRepository, never()).save(any(Lead.class));
    }
}
