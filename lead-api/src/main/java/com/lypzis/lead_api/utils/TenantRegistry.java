package com.lypzis.lead_api.utils;

import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class TenantRegistry {

    // TODO this is a temporary store, use db table later
    private static final Set<String> VALID_KEYS = Set.of(
            "pizza_123",
            "gym_456");

    public boolean isValid(String apiKey) {
        return VALID_KEYS.contains(apiKey);
    }
}
