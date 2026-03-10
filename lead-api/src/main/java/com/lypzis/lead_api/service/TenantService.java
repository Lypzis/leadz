package com.lypzis.lead_api.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
