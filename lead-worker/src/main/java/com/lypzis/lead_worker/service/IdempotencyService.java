package com.lypzis.lead_worker.service;

import org.springframework.stereotype.Service;

import com.lypzis.lead_domain.entity.ProcessedMessage;
import com.lypzis.lead_domain.repository.ProcessedMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedMessageRepository repository;

    public boolean alreadyProcessed(String tenantId, String messageId) {
        return repository.existsByTenantIdAndMessageId(tenantId, messageId);
    }

    public void markProcessed(String tenantId, String messageId) {

        ProcessedMessage record = new ProcessedMessage();
        record.setTenantId(tenantId);
        record.setMessageId(messageId);

        repository.save(record);
    }
}
