package com.lypzis.lead_domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lypzis.lead_domain.entity.Lead;
import com.lypzis.lead_domain.entity.LeadStatus;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByTenantAndPhone(String tenant, String phone);

    Page<Lead> findByTenant(String tenant, Pageable pageable);

    Page<Lead> findByTenantAndStatus(String tenant, LeadStatus status, Pageable pageable);

}
