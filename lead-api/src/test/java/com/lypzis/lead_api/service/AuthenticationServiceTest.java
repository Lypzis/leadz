package com.lypzis.lead_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lypzis.lead_api.dto.AuthResponseDTO;
import com.lypzis.lead_api.dto.LoginRequestDTO;
import com.lypzis.lead_api.exception.UnauthorizedException;
import com.lypzis.lead_domain.entity.AdminUser;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_domain.repository.AdminUserRepository;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void shouldLoginAndReturnTokenWhenCredentialsAreValid() {
        AdminUser adminUser = activeAdmin();
        when(adminUserRepository.findByEmail("admin@tenant.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("secret-123", "stored-hash")).thenReturn(true);
        when(jwtTokenService.generateToken(adminUser)).thenReturn("jwt-token");
        when(jwtTokenService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponseDTO response = authenticationService.login(new LoginRequestDTO("Admin@Tenant.com", "secret-123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);
        verify(adminUserRepository).findByEmail("admin@tenant.com");
    }

    @Test
    void shouldRejectWhenPasswordIsInvalid() {
        AdminUser adminUser = activeAdmin();
        when(adminUserRepository.findByEmail("admin@tenant.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("wrong-pass", "stored-hash")).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authenticationService.login(new LoginRequestDTO("admin@tenant.com", "wrong-pass")));

        assertThat(exception.getMessage()).isEqualTo("Invalid credentials");
    }

    @Test
    void shouldRejectWhenAdminIsInactive() {
        AdminUser adminUser = activeAdmin();
        adminUser.setActive(false);
        when(adminUserRepository.findByEmail("admin@tenant.com")).thenReturn(Optional.of(adminUser));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authenticationService.login(new LoginRequestDTO("admin@tenant.com", "secret-123")));

        assertThat(exception.getMessage()).isEqualTo("Invalid credentials");
    }

    private AdminUser activeAdmin() {
        Tenant tenant = new Tenant();
        tenant.setId(10L);
        tenant.setApiKey("tenant-api-key");
        tenant.setActive(true);

        AdminUser adminUser = new AdminUser();
        adminUser.setId(1L);
        adminUser.setEmail("admin@tenant.com");
        adminUser.setPasswordHash("stored-hash");
        adminUser.setTenant(tenant);
        adminUser.setActive(true);
        return adminUser;
    }
}
