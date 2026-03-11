package com.lypzis.lead_api.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lypzis.lead_api.service.LeadQueryService;
import com.lypzis.lead_contracts.dto.LeadStatusEnum;
import com.lypzis.lead_domain.entity.Lead;
import com.lypzis.lead_domain.entity.LeadMessage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Lead read operations")
@SecurityRequirement(name = "bearerAuth")
public class LeadController {

    private final LeadQueryService service;

    @GetMapping
    @Operation(
            summary = "List leads",
            description = "Returns paginated leads for the authenticated tenant with optional status filter")
    public Page<Lead> listLeads(
            @RequestParam(required = false) LeadStatusEnum status,
            Pageable pageable) {

        return service.listLeads(status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get lead by id",
            description = "Returns one lead by id for the authenticated tenant")
    public Lead getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/{leadId}/timeline")
    @Operation(
            summary = "Get lead timeline",
            description = "Returns conversation timeline for a lead in the authenticated tenant")
    public List<LeadMessage> timeline(@PathVariable Long leadId) {
        return service.getTimeline(leadId);
    }
}
