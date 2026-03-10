package com.lypzis.lead_api.dto;

import java.time.Instant;

import com.lypzis.lead_contracts.dto.AutomationActionTypeEnum;

public record AutomationRuleResponseDTO(
        Long id,
        String tenantId,
        String keyword,
        Integer priority,
        AutomationActionTypeEnum actionType,
        String actionPayload,
        Instant createdAt) {
}
