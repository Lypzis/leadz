package com.lypzis.lead_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class TenantRateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TenantRateLimiterService service;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Test
    void shouldAllowAndSetTtlOnFirstHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean allowed = service.allow("tenant-key", 60);

        assertThat(allowed).isTrue();
        verify(valueOperations).increment(keyCaptor.capture());
        String key = keyCaptor.getValue();
        assertThat(key).startsWith("rate_limit:tenant-key:");
        verify(redisTemplate).expire(key, Duration.ofMinutes(1));
    }

    @Test
    void shouldDenyWhenLimitExceeded() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(61L);

        boolean allowed = service.allow("tenant-key", 60);

        assertThat(allowed).isFalse();
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void shouldDenyWhenCounterIsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(null);

        boolean allowed = service.allow("tenant-key", 60);

        assertThat(allowed).isFalse();
    }

    @Test
    void shouldFailOpenWhenRedisIsUnavailable() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        boolean allowed = service.allow("tenant-key", 60);

        assertThat(allowed).isTrue();
    }
}
