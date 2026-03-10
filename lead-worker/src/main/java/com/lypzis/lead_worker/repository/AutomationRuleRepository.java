package com.lypzis.lead_worker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lypzis.lead_worker.entity.AutomationRule;

public interface AutomationRuleRepository extends JpaRepository<AutomationRule, Long> {

    List<AutomationRule> findByTenant(String tenant);
}
