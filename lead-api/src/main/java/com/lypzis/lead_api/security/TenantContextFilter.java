package com.lypzis.lead_api.security;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.lypzis.lead_api.exception.UnauthorizedException;
import com.lypzis.lead_api.service.TenantService;
import com.lypzis.lead_domain.entity.Tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final TenantService tenantService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean requiresTenant = requiresTenantContext(path);
        String apiKey = request.getHeader(API_KEY_HEADER);

        try {
            if (requiresTenant) {
                if (apiKey == null || apiKey.isBlank()) {
                    handlerExceptionResolver.resolveException(
                            request,
                            response,
                            null,
                            new UnauthorizedException("Missing X-API-Key header"));
                    return;
                }

                Tenant tenant = tenantService.resolveTenant(apiKey);
                TenantContext.set(tenant.getId());
            }

            filterChain.doFilter(request, response);
        } catch (RuntimeException ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean requiresTenantContext(String path) {
        return path.startsWith("/webhook/")
                || path.startsWith("/leads")
                || path.startsWith("/automation-rules");
    }
}
