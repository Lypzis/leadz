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

    public Optional<AutomationRule> matchRule(String message) {

        List<AutomationRule> rules = ruleRepository.findAll();

        for (AutomationRule rule : rules) {
            if (message.toLowerCase().contains(rule.getKeyword().toLowerCase())) {
                return Optional.of(rule);
            }
        }

        return Optional.empty();
    }
}
