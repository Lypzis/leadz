package com.lypzis.lead_worker.service;

import org.springframework.stereotype.Service;

import com.lypzis.lead_domain.entity.ProcessedMessage;
import com.lypzis.lead_domain.repository.ProcessedMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedMessageRepository repository;

    public boolean alreadyProcessed(String tenant, String messageId) {
        return repository.existsByTenantAndMessageId(tenant, messageId);
    }

    public void markProcessed(String tenant, String messageId) {

        ProcessedMessage record = new ProcessedMessage();
        record.setTenant(tenant);
        record.setMessageId(messageId);

        repository.save(record);
    }
}