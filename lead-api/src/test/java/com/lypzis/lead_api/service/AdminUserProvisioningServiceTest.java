package com.lypzis.lead_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lypzis.lead_domain.entity.AdminUser;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_domain.repository.AdminUserRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserProvisioningServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserProvisioningService service;

    @Captor
    private ArgumentCaptor<AdminUser> adminUserCaptor;

    @Test
    void shouldHashPasswordAndPersistAdminUser() {
        Tenant tenant = tenant();
        when(adminUserRepository.existsByEmail("admin@tenant.com")).thenReturn(false);
        when(passwordEncoder.encode("super-secret")).thenReturn("hashed-value");
        when(adminUserRepository.save(any(AdminUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUser result = service.createInitialAdmin(tenant, "  Admin@Tenant.com ", "super-secret");

        assertThat(result.getEmail()).isEqualTo("admin@tenant.com");
        assertThat(result.getPasswordHash()).isEqualTo("hashed-value");
        assertThat(result.getTenant()).isSameAs(tenant);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(adminUserRepository.existsByEmail("admin@tenant.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createInitialAdmin(tenant(), "admin@tenant.com", "super-secret"));

        assertThat(exception.getMessage()).isEqualTo("Admin email already exists");
    }

    @Test
    void shouldRejectShortPassword() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createInitialAdmin(tenant(), "admin@tenant.com", "short"));

        assertThat(exception.getMessage()).isEqualTo("adminPassword must have at least 8 characters");
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setApiKey("tenant-key");
        tenant.setActive(true);
        return tenant;
    }
}
