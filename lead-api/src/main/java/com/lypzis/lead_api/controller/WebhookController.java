package com.lypzis.lead_api.controller;

import com.lypzis.lead_api.dto.LeadEventDTO;
import com.lypzis.lead_api.service.LeadPublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
public class WebhookController {

    private final LeadPublisherService publisherService;

    @PostMapping
    public ResponseEntity<Void> receiveLead(@Valid @RequestBody LeadEventDTO event) {

        publisherService.publish(event);

        return ResponseEntity.accepted().build();
    }
}