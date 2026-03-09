package com.lypzis.lead_worker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lypzis.lead_worker.entity.Lead;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByMessageId(String messageId);
}