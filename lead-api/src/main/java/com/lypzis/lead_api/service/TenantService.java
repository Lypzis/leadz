package com.lypzis.lead_api.service;

import org.springframework.stereotype.Service;

import com.lypzis.lead_api.exception.UnauthorizedException;
import com.lypzis.lead_api.security.TenantContext;
import com.lypzis.lead_domain.entity.Tenant;
import com.lypzis.lead_domain.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    public Tenant resolveTenant(String apiKey) {

        return tenantRepository.findByApiKey(apiKey)
                .filter(Tenant::getActive)
                .orElseThrow(() -> new UnauthorizedException("Invalid or inactive API key"));
    }

    public Tenant getCurrentTenant() {
        Long tenantId = TenantContext.get();

        if (tenantId == null) {
            throw new UnauthorizedException("Tenant context not available");
        }

        return tenantRepository.findById(tenantId)
                .filter(Tenant::getActive)
                .orElseThrow(() -> new UnauthorizedException("Invalid or inactive API key"));
    }

    public Tenant resolveWebhookTenantByPhoneNumberId(String whatsappPhoneNumberId) {
        if (whatsappPhoneNumberId == null || whatsappPhoneNumberId.isBlank()) {
            throw new UnauthorizedException("Missing WhatsApp phone number id in webhook payload");
        }

        return tenantRepository.findByWhatsappPhoneNumberId(whatsappPhoneNumberId)
                .filter(Tenant::getActive)
                .orElseThrow(() -> new UnauthorizedException("Invalid or inactive tenant for webhook payload"));
    }
}
