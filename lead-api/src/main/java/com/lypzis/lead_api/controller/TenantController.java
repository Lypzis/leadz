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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Tenant lifecycle and credentials")
public class TenantController {

    private final TenantManagementService tenantManagementService;

    @PostMapping
    @Operation(
            summary = "Create tenant",
            description = "Creates a new tenant and provisions its initial admin user")
    @SecurityRequirements
    public ResponseEntity<TenantResponseDTO> create(@RequestBody CreateTenantRequestDTO request) {
        TenantResponseDTO response = tenantManagementService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get tenant by id",
            description = "Returns tenant details for an owned tenant")
    @SecurityRequirement(name = "bearerAuth")
    public TenantResponseDTO getById(@PathVariable Long id) {
        return tenantManagementService.getById(id);
    }

    @GetMapping
    @Operation(
            summary = "List tenants",
            description = "Lists tenants accessible by the authenticated admin")
    @SecurityRequirement(name = "bearerAuth")
    public List<TenantResponseDTO> list() {
        return tenantManagementService.list();
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(
            summary = "Deactivate tenant",
            description = "Deactivates a tenant by id")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        tenantManagementService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/regenerate-api-key")
    @Operation(
            summary = "Regenerate tenant API key",
            description = "Generates and returns a new API key for a tenant")
    @SecurityRequirement(name = "bearerAuth")
    public TenantResponseDTO regenerateApiKey(@PathVariable Long id) {
        return tenantManagementService.regenerateApiKey(id);
    }
}
