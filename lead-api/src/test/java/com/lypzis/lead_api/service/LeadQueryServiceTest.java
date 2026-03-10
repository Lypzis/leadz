package com.lypzis.lead_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lypzis.lead_api.exception.ResourceNotFoundException;
import com.lypzis.lead_contracts.dto.LeadStatusEnum;
import com.lypzis.lead_domain.entity.Lead;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_domain.repository.LeadMessageRepository;
import com.lypzis.lead_domain.repository.LeadRepository;

@ExtendWith(MockitoExtension.class)
class LeadQueryServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadMessageRepository leadMessageRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private LeadQueryService leadQueryService;

    @Test
    void getByIdShouldReturnLeadWhenFound() {
        Lead lead = Lead.builder()
                .tenantId("tenant-a")
                .phone("+15550001111")
                .status(LeadStatusEnum.NEW)
                .campaign("cmp-a")
                .build();
        lead.setId(10L);

        when(tenantService.getCurrentTenant()).thenReturn(tenant("tenant-a"));
        when(leadRepository.findByIdAndTenantId(10L, "tenant-a")).thenReturn(Optional.of(lead));

        Lead result = leadQueryService.getById(10L);

        assertThat(result).isSameAs(lead);
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(tenantService.getCurrentTenant()).thenReturn(tenant("tenant-a"));
        when(leadRepository.findByIdAndTenantId(99L, "tenant-a")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> leadQueryService.getById(99L));

        assertThat(exception.getMessage()).isEqualTo("Lead not found: 99");
    }

    private Tenant tenant(String apiKey) {
        Tenant tenant = new Tenant();
        tenant.setApiKey(apiKey);
        tenant.setActive(true);
        return tenant;
    }
}
