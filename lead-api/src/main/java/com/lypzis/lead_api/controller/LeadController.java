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
import com.lypzis.lead_domain.entity.Lead;
import com.lypzis.lead_domain.entity.LeadMessage;
import com.lypzis.lead_domain.entity.LeadStatus;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadQueryService service;

    @GetMapping
    public Page<Lead> listLeads(
            @RequestParam String tenant,
            @RequestParam(required = false) LeadStatus status,
            Pageable pageable) {

        return service.listLeads(tenant, status, pageable);
    }

    @GetMapping("/{leadId}/timeline")
    public List<LeadMessage> timeline(@PathVariable Long leadId) {
        return service.getTimeline(leadId);
    }
}
