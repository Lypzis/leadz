package com.lypzis.lead_domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lypzis.lead_contracts.dto.LeadStatusEnum;
import com.lypzis.lead_domain.entity.Lead;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByIdAndTenantId(Long id, String tenantId);

    Optional<Lead> findByTenantIdAndPhone(String tenantId, String phone);

    Page<Lead> findByTenantId(String tenantId, Pageable pageable);

    Page<Lead> findByTenantIdAndStatus(String tenantId, LeadStatusEnum status, Pageable pageable);

}
