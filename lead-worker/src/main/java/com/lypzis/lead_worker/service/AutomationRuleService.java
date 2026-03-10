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

        // TODO add findByTenantAndKeywordIn, to avoid all rules every message
        List<AutomationRule> rules = ruleRepository.findByTenant(tenant);

        String lowerMessage = message.toLowerCase();

        for (AutomationRule rule : rules) {

            if (lowerMessage.contains(rule.getKeyword().toLowerCase())) {
                return Optional.of(rule);
            }

        }

        return Optional.empty();
    }
}
