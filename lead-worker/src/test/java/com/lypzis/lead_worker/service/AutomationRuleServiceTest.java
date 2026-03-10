package com.lypzis.lead_worker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lypzis.lead_contracts.dto.AutomationActionTypeEnum;
import com.lypzis.lead_domain.entity.AutomationRule;
import com.lypzis.lead_domain.repository.AutomationRuleRepository;

@ExtendWith(MockitoExtension.class)
class AutomationRuleServiceTest {

    @Mock
    private AutomationRuleRepository ruleRepository;

    @InjectMocks
    private AutomationRuleService automationRuleService;

    @Test
    void matchRuleShouldReturnEmptyWhenNoRuleMatches() {
        when(ruleRepository.findByTenantOrderByPriorityDesc("tenant-a"))
                .thenReturn(List.of(
                        rule("tenant-a", "sale", 10),
                        rule("tenant-a", "help", 8)));

        Optional<AutomationRule> result = automationRuleService.matchRule(
                "tenant-a",
                "just saying hello there");

        assertThat(result).isEmpty();
        verify(ruleRepository).findByTenantOrderByPriorityDesc("tenant-a");
    }

    @Test
    void matchRuleShouldBeCaseInsensitive() {
        AutomationRule promoRule = rule("tenant-a", "promo", 7);
        when(ruleRepository.findByTenantOrderByPriorityDesc("tenant-a"))
                .thenReturn(List.of(promoRule));

        Optional<AutomationRule> result = automationRuleService.matchRule(
                "tenant-a",
                "Need a PROMO code please");

        assertThat(result).contains(promoRule);
    }

    @Test
    void matchRuleShouldReturnFirstMatchingRuleFromOrderedList() {
        AutomationRule highPriorityRule = rule("tenant-a", "lead", 20);
        AutomationRule lowPriorityRule = rule("tenant-a", "lead", 5);

        when(ruleRepository.findByTenantOrderByPriorityDesc("tenant-a"))
                .thenReturn(List.of(highPriorityRule, lowPriorityRule));

        Optional<AutomationRule> result = automationRuleService.matchRule(
                "tenant-a",
                "new LEAD arrived");

        assertThat(result).contains(highPriorityRule);
    }

    private AutomationRule rule(String tenant, String keyword, int priority) {
        return AutomationRule.builder()
                .tenant(tenant)
                .keyword(keyword)
                .priority(priority)
                .actionType(AutomationActionTypeEnum.SEND_MESSAGE)
                .actionPayload("payload")
                .build();
    }
}
