package com.lypzis.lead_api.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantRateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public boolean allow(String apiKey, int limitPerMinute) {

        try {

            String bucket = Instant.now()
                    .truncatedTo(ChronoUnit.MINUTES)
                    .toString();

            String key = "rate_limit:" + apiKey + ":" + bucket;

            Long count = redisTemplate.opsForValue().increment(key);

            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }

            return count != null && count <= limitPerMinute;

        } catch (Exception e) {

            log.warn("Redis unavailable, skipping rate limit");

            return true; // fail open
        }
    }
}
