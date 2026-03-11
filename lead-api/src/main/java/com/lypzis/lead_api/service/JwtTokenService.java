package com.lypzis.lead_api.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lypzis.lead_api.exception.UnauthorizedException;
import com.lypzis.lead_domain.entity.AdminUser;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtTokenService {

    private static final String INVALID_TOKEN_MESSAGE = "Invalid or expired token";

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtTokenService(
            @Value("${JWT_SECRET}") String jwtSecret,
            @Value("${JWT_EXPIRATION_SECONDS:3600}") String expirationSecondsRaw) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = parseExpirationSeconds(expirationSecondsRaw);
    }

    private long parseExpirationSeconds(String expirationSecondsRaw) {
        if (expirationSecondsRaw == null || expirationSecondsRaw.isBlank()) {
            return 3600L;
        }

        try {
            return Long.parseLong(expirationSecondsRaw);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("JWT_EXPIRATION_SECONDS must be a number", ex);
        }
    }

    public String generateToken(AdminUser adminUser) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(adminUser.getId().toString())
                .claim("email", adminUser.getEmail())
                .claim("tenantId", adminUser.getTenant().getId())
                .claim("tenantApiKey", adminUser.getTenant().getApiKey())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long extractTenantId(String token) {
        Object claim = parseClaims(token).get("tenantId");

        if (claim instanceof Number number) {
            return number.longValue();
        }
        if (claim instanceof String value && !value.isBlank()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                throw new UnauthorizedException(INVALID_TOKEN_MESSAGE);
            }
        }
        throw new UnauthorizedException(INVALID_TOKEN_MESSAGE);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException(INVALID_TOKEN_MESSAGE);
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
