package com.lypzis.lead_contracts.dto;

public record CreateTenantRequestDTO(
        String name,
        TenantPlanEnum plan,
        Integer requestsPerMinute,
        String adminEmail,
        String adminPassword) {
}
