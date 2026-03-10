package com.lypzis.lead_worker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lypzis.lead_contracts.dto.MessageDirectionEnum;
import com.lypzis.lead_domain.entity.Lead;
import com.lypzis.lead_domain.entity.LeadMessage;
import com.lypzis.lead_domain.entity.LeadStatus;
import com.lypzis.lead_domain.repository.LeadMessageRepository;

@ExtendWith(MockitoExtension.class)
class LeadMessageServiceTest {

    @Mock
    private LeadMessageRepository leadMessageRepository;

    @InjectMocks
    private LeadMessageService leadMessageService;

    @Captor
    private ArgumentCaptor<LeadMessage> messageCaptor;

    @Test
    void saveInboundShouldPersistInboundMessage() {
        Lead lead = sampleLead();
        when(leadMessageRepository.save(any(LeadMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeadMessage result = leadMessageService.saveInbound(
                lead,
                "tenant-a",
                "msg-in-001",
                "inbound content");

        verify(leadMessageRepository).save(messageCaptor.capture());
        LeadMessage saved = messageCaptor.getValue();
        assertThat(saved.getLead()).isSameAs(lead);
        assertThat(saved.getTenant()).isEqualTo("tenant-a");
        assertThat(saved.getMessageId()).isEqualTo("msg-in-001");
        assertThat(saved.getDirection()).isEqualTo(MessageDirectionEnum.INBOUND);
        assertThat(saved.getContent()).isEqualTo("inbound content");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void saveOutboundShouldPersistOutboundMessage() {
        Lead lead = sampleLead();
        when(leadMessageRepository.save(any(LeadMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeadMessage result = leadMessageService.saveOutbound(
                lead,
                "tenant-a",
                "outbound content");

        verify(leadMessageRepository).save(messageCaptor.capture());
        LeadMessage saved = messageCaptor.getValue();
        assertThat(saved.getLead()).isSameAs(lead);
        assertThat(saved.getTenant()).isEqualTo("tenant-a");
        assertThat(saved.getMessageId()).startsWith("out-");
        assertThat(saved.getDirection()).isEqualTo(MessageDirectionEnum.OUTBOUND);
        assertThat(saved.getContent()).isEqualTo("outbound content");
        assertThat(result).isSameAs(saved);
    }

    private Lead sampleLead() {
        Lead lead = Lead.builder()
                .tenant("tenant-a")
                .phone("+15550000001")
                .status(LeadStatus.NEW)
                .campaign("cmp-a")
                .build();
        lead.setId(1L);
        return lead;
    }
}
