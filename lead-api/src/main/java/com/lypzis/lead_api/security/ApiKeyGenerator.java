package com.lypzis.lead_api.security;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class ApiKeyGenerator {

    private static final int KEY_BYTES = 32;
    private static final String PREFIX = "lk_live_";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_SAFE_BASE64 = Base64.getUrlEncoder().withoutPadding();

    public String generate() {
        byte[] random = new byte[KEY_BYTES];
        SECURE_RANDOM.nextBytes(random);
        return PREFIX + URL_SAFE_BASE64.encodeToString(random);
    }
}
