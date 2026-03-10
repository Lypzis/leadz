package com.lypzis.lead_domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lypzis.lead_domain.entity.ProcessedMessage;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, Long> {

    boolean existsByTenantAndMessageId(String tenant, String messageId);
}