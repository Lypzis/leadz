package com.lypzis.lead_worker.service;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lypzis.lead_contracts.dto.AutomationActionTypeEnum;
import com.lypzis.lead_worker.entity.AutomationRule;
import com.lypzis.lead_worker.entity.Lead;
import com.lypzis.lead_worker.repository.LeadRepository;

@ExtendWith(MockitoExtension.class)
class AutomationExecutorTest {

    @Mock
    private MessageSender sender;

    @Mock
    private LeadMessageService leadMessageService;

    @Mock
    private LeadRepository leadRepository;

    @InjectMocks
    private AutomationExecutor automationExecutor;

    @Test
    void executeShouldSendMessageAndSaveOutboundWhenActionIsSendMessage() {
        Lead lead = sampleLead();
        AutomationRule rule = rule(AutomationActionTypeEnum.SEND_MESSAGE, "hello back");

        automationExecutor.execute(rule, lead, "+15550001111");

        verify(sender).sendMessage("+15550001111", "hello back");
        verify(leadMessageService).saveOutbound(lead, "tenant-a", "hello back");
        verify(leadRepository, never()).save(any(Lead.class));
    }

    @Test
    void executeShouldChangeStatusAndPersistWhenActionIsChangeStatus() {
        Lead lead = sampleLead();
        AutomationRule rule = rule(AutomationActionTypeEnum.CHANGE_STATUS, "QUALIFIED");
        when(leadRepository.save(lead)).thenReturn(lead);

        automationExecutor.execute(rule, lead, "+15550001111");

        assertThat(lead.getStatus()).isEqualTo("QUALIFIED");
        verify(leadRepository).save(lead);
        verifyNoInteractions(sender, leadMessageService);
    }

    @Test
    void executeShouldDoNothingWhenActionIsTagLead() {
        Lead lead = sampleLead();
        AutomationRule rule = rule(AutomationActionTypeEnum.TAG_LEAD, "vip");

        automationExecutor.execute(rule, lead, "+15550001111");

        verifyNoInteractions(sender, leadMessageService, leadRepository);
    }

    @Test
    void executeShouldDoNothingWhenActionTypeIsNull() {
        Lead lead = sampleLead();
        AutomationRule rule = rule(null, "payload");

        automationExecutor.execute(rule, lead, "+15550001111");

        verifyNoInteractions(sender, leadMessageService, leadRepository);
    }

    @Test
    void executeShouldNotSendWhenSendMessagePayloadIsBlank() {
        Lead lead = sampleLead();
        AutomationRule rule = rule(AutomationActionTypeEnum.SEND_MESSAGE, "   ");

        automationExecutor.execute(rule, lead, "+15550001111");

        verifyNoInteractions(sender, leadMessageService, leadRepository);
    }

    @Test
    void executeShouldNotChangeStatusWhenChangeStatusPayloadIsBlank() {
        Lead lead = sampleLead();
        AutomationRule rule = rule(AutomationActionTypeEnum.CHANGE_STATUS, " ");

        automationExecutor.execute(rule, lead, "+15550001111");

        assertThat(lead.getStatus()).isEqualTo("NEW");
        verifyNoInteractions(sender, leadMessageService, leadRepository);
    }

    private Lead sampleLead() {
        return Lead.builder()
                .tenant("tenant-a")
                .phone("+15550001111")
                .status("NEW")
                .campaign("cmp-a")
                .build();
    }

    private AutomationRule rule(AutomationActionTypeEnum type, String payload) {
        return AutomationRule.builder()
                .tenant("tenant-a")
                .keyword("hello")
                .priority(10)
                .actionType(type)
                .actionPayload(payload)
                .build();
    }
}
