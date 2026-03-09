package com.lypzis.lead_worker.service;

import com.lypzis.lead_worker.dto.LeadEventDTO;
import com.lypzis.lead_worker.entity.Lead;
import com.lypzis.lead_worker.repository.LeadRepository;
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

        when(leadRepository.findByMessageId("msg-001")).thenReturn(Optional.empty());

        leadListener.handleLead(event);

        verify(leadRepository).save(leadCaptor.capture());
        Lead savedLead = leadCaptor.getValue();
        assertThat(savedLead.getMessageId()).isEqualTo("msg-001");
        assertThat(savedLead.getPhone()).isEqualTo("+15551234567");
        assertThat(savedLead.getMessage()).isEqualTo("hello");
        assertThat(savedLead.getCampaign()).isEqualTo("campaign-a");
    }

    @Test
    void handleLeadShouldIgnoreDuplicateMessage() {
        LeadEventDTO event = new LeadEventDTO();
        event.setMessageId("msg-dup");

        when(leadRepository.findByMessageId("msg-dup"))
                .thenReturn(Optional.of(Lead.builder().id(1L).messageId("msg-dup").build()));

        leadListener.handleLead(event);

        verify(leadRepository, never()).save(any(Lead.class));
    }
}
