package com.lypzis.lead_worker.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lypzis.lead_contracts.dto.MessageDirectionEnum;
import com.lypzis.lead_domain.entity.Lead;
import com.lypzis.lead_domain.entity.LeadMessage;
import com.lypzis.lead_domain.repository.LeadMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeadMessageService {

    private final LeadMessageRepository leadMessageRepository;

    public LeadMessage saveInbound(Lead lead, String tenantId, String messageId, String content) {
        return leadMessageRepository.save(
                LeadMessage.builder()
                        .tenantId(tenantId)
                        .messageId(messageId)
                        .lead(lead)
                        .direction(MessageDirectionEnum.INBOUND)
                        .content(content)
                        .build());
    }

    public LeadMessage saveOutbound(Lead lead, String tenantId, String content) {
        return leadMessageRepository.save(
                LeadMessage.builder()
                        .tenantId(tenantId)
                        .messageId("out-" + UUID.randomUUID())
                        .lead(lead)
                        // TODO For outbound, later add a dedicated response message ID from WhatsApp
                        // provider.
                        .direction(MessageDirectionEnum.OUTBOUND)
                        .content(content)
                        .build());
    }
}
