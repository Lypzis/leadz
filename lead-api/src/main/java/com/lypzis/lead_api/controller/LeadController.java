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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadQueryService service;

    @GetMapping
    public Page<Lead> listLeads(
            @RequestParam(required = false) LeadStatusEnum status,
            Pageable pageable) {

        return service.listLeads(status, pageable);
    }

    @GetMapping("/{id}")
    public Lead getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/{leadId}/timeline")
    public List<LeadMessage> timeline(@PathVariable Long leadId) {
        return service.getTimeline(leadId);
    }
}
