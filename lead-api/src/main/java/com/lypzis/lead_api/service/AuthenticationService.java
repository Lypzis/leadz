package com.lypzis.lead_api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.lypzis.lead_api.dto.AuthResponseDTO;
import com.lypzis.lead_api.dto.LoginRequestDTO;
import com.lypzis.lead_api.exception.UnauthorizedException;
import com.lypzis.lead_domain.entity.AdminUser;
import com.lypzis.lead_domain.repository.AdminUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthResponseDTO login(LoginRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("password is required");
        }

        String normalizedEmail = request.email().trim().toLowerCase();

        AdminUser adminUser = adminUserRepository.findByEmail(normalizedEmail)
                .filter(AdminUser::isActive)
                .filter(user -> user.getTenant() != null && Boolean.TRUE.equals(user.getTenant().getActive()))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), adminUser.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtTokenService.generateToken(adminUser);
        return new AuthResponseDTO(token, "Bearer", jwtTokenService.getExpirationSeconds());
    }
}
