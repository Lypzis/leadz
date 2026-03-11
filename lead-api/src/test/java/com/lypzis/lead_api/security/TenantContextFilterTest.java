package com.lypzis.lead_api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.lypzis.lead_api.service.JwtTokenService;
import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class TenantContextFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldSetTenantContextFromBearerTokenForLeadsEndpoints() throws Exception {
        TenantContextFilter filter = new TenantContextFilter(jwtTokenService, handlerExceptionResolver);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leads");
        request.addHeader("Authorization", "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenService.extractTenantId("jwt-token")).thenReturn(42L);

        AtomicReference<Long> tenantInChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> tenantInChain.set(TenantContext.get());

        filter.doFilter(request, response, chain);

        assertThat(tenantInChain.get()).isEqualTo(42L);
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void shouldRequireBearerTokenForAutomationRuleEndpoints() throws Exception {
        TenantContextFilter filter = new TenantContextFilter(jwtTokenService, handlerExceptionResolver);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/automation-rules");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            throw new AssertionError("Filter chain should not be invoked");
        };

        filter.doFilter(request, response, chain);

        verify(handlerExceptionResolver).resolveException(
                eq(request),
                eq(response),
                eq(null),
                any(RuntimeException.class));
        verify(jwtTokenService, never()).extractTenantId(any());
    }

    @Test
    void shouldNotRequireAuthForWebhookPost() throws Exception {
        TenantContextFilter filter = new TenantContextFilter(jwtTokenService, handlerExceptionResolver);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/webhook/whatsapp");
        request.setContentType("application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Long> tenantInChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> tenantInChain.set(TenantContext.get());

        filter.doFilter(request, response, chain);

        assertThat(tenantInChain.get()).isNull();
        verify(jwtTokenService, never()).extractTenantId(any());
    }

    @Test
    void shouldNotRequireAuthForWebhookVerificationGet() throws Exception {
        TenantContextFilter filter = new TenantContextFilter(jwtTokenService, handlerExceptionResolver);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/webhook/whatsapp");
        request.addParameter("hub.mode", "subscribe");
        request.addParameter("hub.verify_token", "any");
        request.addParameter("hub.challenge", "challenge");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Long> tenantInChain = new AtomicReference<>(-1L);
        FilterChain chain = (req, res) -> tenantInChain.set(TenantContext.get());

        filter.doFilter(request, response, chain);

        assertThat(tenantInChain.get()).isNull();
        verify(jwtTokenService, never()).extractTenantId(any());
    }

    @Test
    void shouldNotRequireAuthForTenantCreationEndpoint() throws Exception {
        TenantContextFilter filter = new TenantContextFilter(jwtTokenService, handlerExceptionResolver);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/tenants");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Long> tenantInChain = new AtomicReference<>(-1L);
        FilterChain chain = (req, res) -> tenantInChain.set(TenantContext.get());

        filter.doFilter(request, response, chain);

        assertThat(tenantInChain.get()).isNull();
        verify(jwtTokenService, never()).extractTenantId(any());
    }
}
