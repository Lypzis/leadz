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
import com.lypzis.lead_contracts.dto.LeadStatusEnum;
import com.lypzis.lead_contracts.dto.ProcessingResultEnum;
import com.lypzis.lead_domain.entity.AutomationRule;
import com.lypzis.lead_domain.entity.Lead;
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

        @Mock
        private IdempotencyService idempotencyService;

        @InjectMocks
        private LeadService leadService;

        @Captor
        private ArgumentCaptor<Lead> leadCaptor;

        @Test
        void findOrCreateLeadShouldReturnExistingLead() {
                Lead existing = Lead.builder()
                                .tenantId("tenant-a")
                                .phone("+15550000001")
                                .status(LeadStatusEnum.QUALIFIED)
                                .campaign("cmp-a")
                                .build();

                when(leadRepository.findByTenantIdAndPhone("tenant-a", "+15550000001"))
                                .thenReturn(Optional.of(existing));

                Lead result = leadService.findOrCreateLead("tenant-a", "+15550000001", "cmp-a");

                assertThat(result).isSameAs(existing);
                verify(leadRepository, never()).save(any(Lead.class));
        }

        @Test
        void findOrCreateLeadShouldCreateLeadWhenNotFound() {
                when(leadRepository.findByTenantIdAndPhone("tenant-a", "+15550000002"))
                                .thenReturn(Optional.empty());

                Lead persisted = Lead.builder()
                                .tenantId("tenant-a")
                                .phone("+15550000002")
                                .status(LeadStatusEnum.NEW)
                                .campaign("cmp-b")
                                .build();
                persisted.setId(10L);

                when(leadRepository.save(any(Lead.class))).thenReturn(persisted);

                Lead result = leadService.findOrCreateLead("tenant-a", "+15550000002", "cmp-b");

                verify(leadRepository).save(leadCaptor.capture());
                Lead saved = leadCaptor.getValue();
                assertThat(saved.getTenantId()).isEqualTo("tenant-a");
                assertThat(saved.getPhone()).isEqualTo("+15550000002");
                assertThat(saved.getStatus()).isEqualTo(LeadStatusEnum.NEW);
                assertThat(saved.getCampaign()).isEqualTo("cmp-b");
                assertThat(result).isSameAs(persisted);
        }

        @Test
        void processLeadShouldSkipUnsupportedVersion() {
                LeadDTO event = sampleEvent();
                event.setVersion(2);

                ProcessingResultEnum result = leadService.processLeadTransactionally(event);

                assertThat(result).isEqualTo(ProcessingResultEnum.UNSUPPORTED_VERSION);
                verify(leadRepository, never()).findByTenantIdAndPhone(any(), any());
                verify(leadRepository, never()).save(any(Lead.class));
                verifyNoInteractions(idempotencyService, ruleService, leadMessageService, automationExecutor);
        }

        @Test
        void processLeadShouldIgnoreDuplicateMessage() {
                LeadDTO event = sampleEvent();
                when(idempotencyService.alreadyProcessed(event.getTenant(), event.getMessageId())).thenReturn(true);

                ProcessingResultEnum result = leadService.processLeadTransactionally(event);

                assertThat(result).isEqualTo(ProcessingResultEnum.DUPLICATE_IGNORED);
                verify(idempotencyService).alreadyProcessed(event.getTenant(), event.getMessageId());
                verify(leadRepository, never()).findByTenantIdAndPhone(any(), any());
                verifyNoInteractions(ruleService, leadMessageService, automationExecutor);
                verify(idempotencyService, never()).markProcessed(any(), any());
        }

        @Test
        void processLeadShouldSaveInboundAndNotExecuteAutomationWhenNoRuleMatches() {
                LeadDTO event = sampleEvent();
                Lead existing = Lead.builder()
                                .tenantId(event.getTenant())
                                .phone(event.getPhone())
                                .status(LeadStatusEnum.NEW)
                                .campaign(event.getCampaign())
                                .build();

                when(leadRepository.findByTenantIdAndPhone(event.getTenant(), event.getPhone()))
                                .thenReturn(Optional.of(existing));
                when(idempotencyService.alreadyProcessed(event.getTenant(), event.getMessageId()))
                                .thenReturn(false);
                when(ruleService.matchRule(event.getTenant(), event.getMessage()))
                                .thenReturn(Optional.empty());

                ProcessingResultEnum result = leadService.processLeadTransactionally(event);

                assertThat(result).isEqualTo(ProcessingResultEnum.PROCESSED);
                verify(idempotencyService).alreadyProcessed(event.getTenant(), event.getMessageId());
                verify(leadRepository).findByTenantIdAndPhone(event.getTenant(), event.getPhone());
                verify(leadMessageService).saveInbound(
                                existing,
                                event.getTenant(),
                                event.getMessageId(),
                                event.getMessage());
                verify(ruleService).matchRule(event.getTenant(), event.getMessage());
                verifyNoInteractions(automationExecutor);
                verify(idempotencyService).markProcessed(event.getTenant(), event.getMessageId());
        }

        @Test
        void processLeadShouldSaveInboundAndExecuteAutomationWhenRuleMatches() {
                LeadDTO event = sampleEvent();
                Lead existing = Lead.builder()
                                .tenantId(event.getTenant())
                                .phone(event.getPhone())
                                .status(LeadStatusEnum.NEW)
                                .campaign(event.getCampaign())
                                .build();
                AutomationRule rule = AutomationRule.builder()
                                .tenantId(event.getTenant())
                                .keyword("hello")
                                .priority(10)
                                .actionType(AutomationActionTypeEnum.SEND_MESSAGE)
                                .actionPayload("auto-reply")
                                .build();

                when(leadRepository.findByTenantIdAndPhone(event.getTenant(), event.getPhone()))
                                .thenReturn(Optional.of(existing));
                when(idempotencyService.alreadyProcessed(event.getTenant(), event.getMessageId()))
                                .thenReturn(false);
                when(ruleService.matchRule(event.getTenant(), event.getMessage()))
                                .thenReturn(Optional.of(rule));

                ProcessingResultEnum result = leadService.processLeadTransactionally(event);

                assertThat(result).isEqualTo(ProcessingResultEnum.PROCESSED);
                InOrder inOrder = inOrder(leadMessageService, automationExecutor, idempotencyService);
                inOrder.verify(leadMessageService).saveInbound(
                                existing,
                                event.getTenant(),
                                event.getMessageId(),
                                event.getMessage());
                inOrder.verify(automationExecutor).execute(
                                rule,
                                existing,
                                event.getPhone());
                inOrder.verify(idempotencyService).markProcessed(event.getTenant(), event.getMessageId());
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
