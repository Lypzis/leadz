package com.lypzis.lead_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lypzis.lead_api.exception.UnauthorizedException;
import com.lypzis.lead_api.security.ApiKeyGenerator;
import com.lypzis.lead_contracts.dto.CreateTenantRequestDTO;
import com.lypzis.lead_contracts.dto.TenantPlanEnum;
import com.lypzis.lead_contracts.dto.TenantResponseDTO;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_domain.repository.TenantRepository;

@ExtendWith(MockitoExtension.class)
class TenantManagementServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ApiKeyGenerator apiKeyGenerator;

    @Mock
    private AdminUserProvisioningService adminUserProvisioningService;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private TenantManagementService tenantManagementService;

    @Test
    void createShouldRequireWhatsappPhoneNumberId() {
        CreateTenantRequestDTO request = new CreateTenantRequestDTO(
                "Tenant A",
                TenantPlanEnum.PRO,
                100,
                " ",
                "admin@tenant.com",
                "password123");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantManagementService.create(request));

        assertThat(exception.getMessage()).isEqualTo("whatsappPhoneNumberId is required");
    }

    @Test
    void createShouldRejectDuplicateWhatsappPhoneNumberId() {
        CreateTenantRequestDTO request = new CreateTenantRequestDTO(
                "Tenant A",
                TenantPlanEnum.PRO,
                100,
                "123456123",
                "admin@tenant.com",
                "password123");

        when(tenantRepository.findByWhatsappPhoneNumberId("123456123"))
                .thenReturn(Optional.of(tenant(1L, "tenant-key-1")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantManagementService.create(request));

        assertThat(exception.getMessage()).isEqualTo("whatsappPhoneNumberId already in use");
        verify(tenantRepository, never()).save(org.mockito.ArgumentMatchers.any(Tenant.class));
    }

    @Test
    void shouldRejectGetByIdWhenTenantIsNotOwnedByCurrentAdmin() {
        Tenant currentTenant = tenant(10L, "tenant-key-10");
        when(tenantService.getCurrentTenant()).thenReturn(currentTenant);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> tenantManagementService.getById(20L));

        assertThat(exception.getMessage()).isEqualTo("Tenant access denied");
        verify(tenantRepository, never()).findById(20L);
    }

    @Test
    void listShouldReturnOnlyCurrentTenant() {
        Tenant currentTenant = tenant(10L, "tenant-key-10");
        when(tenantService.getCurrentTenant()).thenReturn(currentTenant);

        List<TenantResponseDTO> result = tenantManagementService.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(0).apiKey()).isEqualTo("tenant-key-10");
    }

    @Test
    void regenerateApiKeyShouldFailWhenRequestTargetsAnotherTenant() {
        Tenant currentTenant = tenant(10L, "tenant-key-10");
        when(tenantService.getCurrentTenant()).thenReturn(currentTenant);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> tenantManagementService.regenerateApiKey(99L));

        assertThat(exception.getMessage()).isEqualTo("Tenant access denied");
        verify(tenantRepository, never()).save(currentTenant);
    }

    @Test
    void regenerateApiKeyShouldRotateOnlyOwnedTenant() {
        Tenant currentTenant = tenant(10L, "old-key");
        when(tenantService.getCurrentTenant()).thenReturn(currentTenant);
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(currentTenant));
        when(apiKeyGenerator.generate()).thenReturn("new-key");
        when(tenantRepository.save(currentTenant)).thenReturn(currentTenant);

        TenantResponseDTO response = tenantManagementService.regenerateApiKey(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.apiKey()).isEqualTo("new-key");
        verify(tenantRepository).save(currentTenant);
    }

    private Tenant tenant(Long id, String apiKey) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName("Tenant");
        tenant.setApiKey(apiKey);
        tenant.setPlan(TenantPlanEnum.PRO);
        tenant.setRequestsPerMinute(100);
        tenant.setActive(true);
        tenant.setCreatedAt(Instant.now());
        return tenant;
    }
}
