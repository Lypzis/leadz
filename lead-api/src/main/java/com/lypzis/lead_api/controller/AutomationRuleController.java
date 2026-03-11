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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/automation-rules")
@RequiredArgsConstructor
@Tag(name = "Automation Rules", description = "Automation rule management")
@SecurityRequirement(name = "bearerAuth")
public class AutomationRuleController {

    private final AutomationRuleService automationRuleService;

    @PostMapping
    @Operation(
            summary = "Create automation rule",
            description = "Creates a rule that triggers actions when a keyword is detected")
    public ResponseEntity<AutomationRuleResponseDTO> create(@RequestBody AutomationRuleRequestDTO request) {

        AutomationRuleResponseDTO response = automationRuleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List automation rules",
            description = "Lists all automation rules for the authenticated tenant")
    public List<AutomationRuleResponseDTO> list() {

        return automationRuleService.list();
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update automation rule",
            description = "Updates an existing automation rule by id for the authenticated tenant")
    public AutomationRuleResponseDTO update(
            @PathVariable Long id,
            @RequestBody AutomationRuleRequestDTO request) {

        return automationRuleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete automation rule",
            description = "Deletes an automation rule by id for the authenticated tenant")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        automationRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
