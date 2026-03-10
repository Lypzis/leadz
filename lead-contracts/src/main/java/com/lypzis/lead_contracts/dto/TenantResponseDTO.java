package com.lypzis.lead_contracts.dto;

import java.time.Instant;

public record TenantResponseDTO(
        Long id,
        String name,
        String apiKey,
        TenantPlanEnum plan,
        Integer requestsPerMinute,
        boolean active,
        Instant createdAt) {
}