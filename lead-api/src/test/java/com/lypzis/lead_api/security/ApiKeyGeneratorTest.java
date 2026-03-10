package com.lypzis.lead_api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ApiKeyGeneratorTest {

    private static final String PREFIX = "lk_live_";

    private final ApiKeyGenerator apiKeyGenerator = new ApiKeyGenerator();

    @Test
    void generateShouldReturnPrefixedUrlSafeKey() {
        String apiKey = apiKeyGenerator.generate();

        assertThat(apiKey).startsWith(PREFIX);
        assertThat(apiKey).doesNotContain("=");
        assertThat(apiKey.substring(PREFIX.length())).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void generateShouldProduceDifferentValuesAcrossCalls() {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            keys.add(apiKeyGenerator.generate());
        }

        assertThat(keys).hasSize(100);
    }
}
