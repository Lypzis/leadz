package com.lypzis.lead_api.dto;

import com.lypzis.lead_contracts.dto.AutomationActionTypeEnum;

public record AutomationRuleRequestDTO(
        String keyword,
        Integer priority,
        AutomationActionTypeEnum actionType,
        String actionPayload) {
}
