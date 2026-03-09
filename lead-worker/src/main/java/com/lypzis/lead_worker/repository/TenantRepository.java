package com.lypzis.lead_worker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lypzis.lead_worker.entity.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByApiKey(String apiKey);
}
