package com.lypzis.lead_worker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lypzis.lead_contracts.dto.AutomationActionTypeEnum;
import com.lypzis.lead_contracts.dto.LeadDTO;
import com.lypzis.lead_domain.entity.AutomationRule;
import com.lypzis.lead_domain.entity.Lead;
import com.lypzis.lead_domain.entity.LeadStatus;
import com.lypzis.lead_domain.repository.LeadRepository;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private AutomationRuleService ruleService;

    @Mock
    private LeadMessageService leadMessageService;

    @Mock
    private AutomationExecutor automationExecutor;

    @InjectMocks
    private LeadService leadService;

    @Captor
    private ArgumentCaptor<Lead> leadCaptor;

    @Test
    void findOrCreateLeadShouldReturnExistingLead() {
        Lead existing = Lead.builder()
                .tenant("tenant-a")
                .phone("+15550000001")
                .status(LeadStatus.QUALIFIED)
                .campaign("cmp-a")
                .build();

        when(leadRepository.findByTenantAndPhone("tenant-a", "+15550000001"))
                .thenReturn(Optional.of(existing));

        Lead result = leadService.findOrCreateLead("tenant-a", "+15550000001", "cmp-a");

        assertThat(result).isSameAs(existing);
        verify(leadRepository, never()).save(any(Lead.class));
    }

    @Test
    void findOrCreateLeadShouldCreateLeadWhenNotFound() {
        when(leadRepository.findByTenantAndPhone("tenant-a", "+15550000002"))
                .thenReturn(Optional.empty());

        Lead persisted = Lead.builder()
                .tenant("tenant-a")
                .phone("+15550000002")
                .status(LeadStatus.NEW)
                .campaign("cmp-b")
                .build();
        persisted.setId(10L);

        when(leadRepository.save(any(Lead.class))).thenReturn(persisted);

        Lead result = leadService.findOrCreateLead("tenant-a", "+15550000002", "cmp-b");

        verify(leadRepository).save(leadCaptor.capture());
        Lead saved = leadCaptor.getValue();
        assertThat(saved.getTenant()).isEqualTo("tenant-a");
        assertThat(saved.getPhone()).isEqualTo("+15550000002");
        assertThat(saved.getStatus()).isEqualTo(LeadStatus.NEW);
        assertThat(saved.getCampaign()).isEqualTo("cmp-b");
        assertThat(result).isSameAs(persisted);
    }

    @Test
    void processLeadShouldSkipUnsupportedVersion() {
        LeadDTO event = sampleEvent();
        event.setVersion(2);

        leadService.processLead(event);

        verify(leadRepository, never()).findByTenantAndPhone(any(), any());
        verify(leadRepository, never()).save(any(Lead.class));
        verifyNoInteractions(ruleService, leadMessageService, automationExecutor);
    }

    @Test
    void processLeadShouldSaveInboundAndNotExecuteAutomationWhenNoRuleMatches() {
        LeadDTO event = sampleEvent();
        Lead existing = Lead.builder()
                .tenant(event.getTenant())
                .phone(event.getPhone())
                .status(LeadStatus.NEW)
                .campaign(event.getCampaign())
                .build();

        when(leadRepository.findByTenantAndPhone(event.getTenant(), event.getPhone()))
                .thenReturn(Optional.of(existing));
        when(ruleService.matchRule(event.getTenant(), event.getMessage()))
                .thenReturn(Optional.empty());

        leadService.processLead(event);

        verify(leadRepository).findByTenantAndPhone(event.getTenant(), event.getPhone());
        verify(leadMessageService).saveInbound(
                existing,
                event.getTenant(),
                event.getMessageId(),
                event.getMessage());
        verify(ruleService).matchRule(event.getTenant(), event.getMessage());
        verifyNoInteractions(automationExecutor);
    }

    @Test
    void processLeadShouldSaveInboundAndExecuteAutomationWhenRuleMatches() {
        LeadDTO event = sampleEvent();
        Lead existing = Lead.builder()
                .tenant(event.getTenant())
                .phone(event.getPhone())
                .status(LeadStatus.NEW)
                .campaign(event.getCampaign())
                .build();
        AutomationRule rule = AutomationRule.builder()
                .tenant(event.getTenant())
                .keyword("hello")
                .priority(10)
                .actionType(AutomationActionTypeEnum.SEND_MESSAGE)
                .actionPayload("auto-reply")
                .build();

        when(leadRepository.findByTenantAndPhone(event.getTenant(), event.getPhone()))
                .thenReturn(Optional.of(existing));
        when(ruleService.matchRule(event.getTenant(), event.getMessage()))
                .thenReturn(Optional.of(rule));

        leadService.processLead(event);

        InOrder inOrder = inOrder(leadMessageService, automationExecutor);
        inOrder.verify(leadMessageService).saveInbound(
                existing,
                event.getTenant(),
                event.getMessageId(),
                event.getMessage());
        inOrder.verify(automationExecutor).execute(
                rule,
                existing,
                event.getPhone());
    }

    private LeadDTO sampleEvent() {
        LeadDTO event = new LeadDTO();
        event.setVersion(1);
        event.setMessageId("msg-001");
        event.setPhone("+15550000001");
        event.setMessage("hello");
        event.setCampaign("cmp-a");
        event.setTenant("tenant-a");
        return event;
    }
}
