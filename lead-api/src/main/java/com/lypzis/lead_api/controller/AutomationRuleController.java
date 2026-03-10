package com.lypzis.lead_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lypzis.lead_api.dto.AutomationRuleRequestDTO;
import com.lypzis.lead_api.dto.AutomationRuleResponseDTO;
import com.lypzis.lead_api.service.AutomationRuleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/automation-rules")
@RequiredArgsConstructor
public class AutomationRuleController {

    private final AutomationRuleService automationRuleService;

    @PostMapping
    public ResponseEntity<AutomationRuleResponseDTO> create(@RequestBody AutomationRuleRequestDTO request) {

        AutomationRuleResponseDTO response = automationRuleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<AutomationRuleResponseDTO> list() {

        return automationRuleService.list();
    }

    @PutMapping("/{id}")
    public AutomationRuleResponseDTO update(
            @PathVariable Long id,
            @RequestBody AutomationRuleRequestDTO request) {

        return automationRuleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        automationRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
