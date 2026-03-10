package com.lypzis.lead_domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lypzis.lead_domain.entity.LeadMessage;

@Repository
public interface LeadMessageRepository extends JpaRepository<LeadMessage, Long> {

    // TODO for timeline display
    List<LeadMessage> findByLeadIdOrderByCreatedAtAsc(Long leadId);
}
