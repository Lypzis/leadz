package com.lypzis.lead_domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lypzis.lead_domain.entity.Lead;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByTenantAndPhone(String tenant, String phone);

}
