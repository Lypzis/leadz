package com.lypzis.lead_worker.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.lypzis.lead_worker.entity.AutomationRule;
import com.lypzis.lead_worker.repository.AutomationRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutomationRuleService {

    private final AutomationRuleRepository ruleRepository;

    public Optional<AutomationRule> matchRule(String tenant, String message) {

        String msg = message.toLowerCase();

        List<AutomationRule> rules = ruleRepository.findByTenantOrderByPriorityDesc(tenant);

        for (AutomationRule rule : rules) {

            String keyword = rule.getKeyword().toLowerCase();

            if (msg.contains(keyword)) {
                return Optional.of(rule);
            }
        }

        return Optional.empty();
    }
}
