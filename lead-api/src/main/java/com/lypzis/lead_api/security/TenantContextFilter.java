package com.lypzis.lead_api.security;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.lypzis.lead_api.exception.UnauthorizedException;
import com.lypzis.lead_api.service.JwtTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean requiresAdminJwt = requiresAdminJwt(path, request.getMethod());
        String authorization = request.getHeader(AUTHORIZATION_HEADER);

        try {
            if (requiresAdminJwt) {
                String token = extractBearerToken(authorization);
                if (token == null) {
                    handlerExceptionResolver.resolveException(
                            request,
                            response,
                            null,
                            new UnauthorizedException("Missing Authorization Bearer token"));
                    return;
                }
                Long tenantId = jwtTokenService.extractTenantId(token);
                TenantContext.set(tenantId);
            }

            filterChain.doFilter(request, response);
        } catch (RuntimeException ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean requiresAdminJwt(String path, String method) {
        if (path.startsWith("/leads") || path.startsWith("/automation-rules")) {
            return true;
        }
        if (path.equals("/tenants") && "POST".equalsIgnoreCase(method)) {
            return false;
        }
        return path.startsWith("/tenants");
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isBlank() ? null : token;
    }
}
