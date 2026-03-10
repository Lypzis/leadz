package com.lypzis.lead_api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.lypzis.lead_domain.entity.AdminUser;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_domain.repository.AdminUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserProvisioningService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUser createInitialAdmin(Tenant tenant, String email, String rawPassword) {
        if (tenant == null || tenant.getId() == null) {
            throw new IllegalArgumentException("Tenant must be persisted before admin provisioning");
        }

        String normalizedEmail = normalizeEmail(email);
        validatePassword(rawPassword);

        if (adminUserRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Admin email already exists");
        }

        AdminUser adminUser = AdminUser.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .tenant(tenant)
                .active(true)
                .build();

        return adminUserRepository.save(adminUser);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("adminEmail is required");
        }
        return email.trim().toLowerCase();
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("adminPassword is required");
        }
        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException("adminPassword must have at least 8 characters");
        }
    }
}
