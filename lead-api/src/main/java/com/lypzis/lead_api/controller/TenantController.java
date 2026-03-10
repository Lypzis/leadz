package com.lypzis.lead_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lypzis.lead_api.service.TenantManagementService;
import com.lypzis.lead_contracts.dto.CreateTenantRequestDTO;
import com.lypzis.lead_contracts.dto.TenantResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantManagementService tenantManagementService;

    @PostMapping
    public ResponseEntity<TenantResponseDTO> create(@RequestBody CreateTenantRequestDTO request) {
        TenantResponseDTO response = tenantManagementService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public TenantResponseDTO getById(@PathVariable Long id) {
        return tenantManagementService.getById(id);
    }

    @GetMapping
    public List<TenantResponseDTO> list() {
        return tenantManagementService.list();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        tenantManagementService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/regenerate-api-key")
    public TenantResponseDTO regenerateApiKey(@PathVariable Long id) {
        return tenantManagementService.regenerateApiKey(id);
    }
}
