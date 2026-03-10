package com.lypzis.lead_domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lypzis.lead_domain.entity.AutomationRule;

public interface AutomationRuleRepository extends JpaRepository<AutomationRule, Long> {

    List<AutomationRule> findByTenantIdOrderByPriorityDesc(String tenantId);

    Optional<AutomationRule> findByIdAndTenantId(Long id, String tenantId);
}
