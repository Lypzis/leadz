package com.lypzis.lead_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lypzis.lead_api.dto.AutomationRuleRequestDTO;
import com.lypzis.lead_api.dto.AutomationRuleResponseDTO;
import com.lypzis.lead_api.exception.ResourceNotFoundException;
import com.lypzis.lead_contracts.dto.AutomationActionTypeEnum;
import com.lypzis.lead_contracts.dto.TenantPlanEnum;
import com.lypzis.lead_domain.entity.AutomationRule;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_domain.repository.AutomationRuleRepository;

@ExtendWith(MockitoExtension.class)
class AutomationRuleServiceTest {

    @Mock
    private AutomationRuleRepository automationRuleRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private AutomationRuleService automationRuleService;

    @Captor
    private ArgumentCaptor<AutomationRule> ruleCaptor;

    @Test
    void createShouldPersistRuleScopedByTenant() {
        when(tenantService.getCurrentTenant()).thenReturn(activeTenant("tenant-a"));
        when(automationRuleRepository.save(any(AutomationRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AutomationRuleResponseDTO response = automationRuleService.create(
                request("hello", 10, AutomationActionTypeEnum.SEND_MESSAGE, " auto "));

        verify(automationRuleRepository).save(ruleCaptor.capture());
        AutomationRule saved = ruleCaptor.getValue();
        assertThat(saved.getTenantId()).isEqualTo("tenant-a");
        assertThat(saved.getKeyword()).isEqualTo("hello");
        assertThat(saved.getPriority()).isEqualTo(10);
        assertThat(saved.getActionType()).isEqualTo(AutomationActionTypeEnum.SEND_MESSAGE);
        assertThat(saved.getActionPayload()).isEqualTo("auto");
        assertThat(response.tenantId()).isEqualTo("tenant-a");
    }

    @Test
    void listShouldReturnOnlyResolvedTenantRules() {
        when(tenantService.getCurrentTenant()).thenReturn(activeTenant("tenant-a"));
        when(automationRuleRepository.findByTenantIdOrderByPriorityDesc("tenant-a"))
                .thenReturn(List.of(AutomationRule.builder()
                        .tenantId("tenant-a")
                        .keyword("promo")
                        .priority(5)
                        .actionType(AutomationActionTypeEnum.SEND_MESSAGE)
                        .actionPayload("payload")
                        .build()));

        List<AutomationRuleResponseDTO> response = automationRuleService.list();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).tenantId()).isEqualTo("tenant-a");
        assertThat(response.get(0).keyword()).isEqualTo("promo");
    }

    @Test
    void updateShouldThrowWhenRuleDoesNotBelongToTenant() {
        when(tenantService.getCurrentTenant()).thenReturn(activeTenant("tenant-a"));
        when(automationRuleRepository.findByIdAndTenantId(11L, "tenant-a"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> automationRuleService.update(
                11L,
                request("hello", 1, AutomationActionTypeEnum.TAG_LEAD, "vip")));
    }

    @Test
    void deleteShouldDeleteOwnedRule() {
        AutomationRule rule = AutomationRule.builder()
                .tenantId("tenant-a")
                .keyword("help")
                .priority(1)
                .actionType(AutomationActionTypeEnum.CHANGE_STATUS)
                .actionPayload("QUALIFIED")
                .build();
        when(tenantService.getCurrentTenant()).thenReturn(activeTenant("tenant-a"));
        when(automationRuleRepository.findByIdAndTenantId(9L, "tenant-a"))
                .thenReturn(Optional.of(rule));

        automationRuleService.delete(9L);

        verify(automationRuleRepository).delete(rule);
    }

    private AutomationRuleRequestDTO request(
            String keyword,
            Integer priority,
            AutomationActionTypeEnum actionType,
            String actionPayload) {
        return new AutomationRuleRequestDTO(keyword, priority, actionType, actionPayload);
    }

    private Tenant activeTenant(String tenantId) {
        Tenant tenant = new Tenant();
        tenant.setApiKey(tenantId);
        tenant.setName("Tenant A");
        tenant.setPlan(TenantPlanEnum.PRO);
        tenant.setRequestsPerMinute(100);
        tenant.setActive(true);
        return tenant;
    }
}
