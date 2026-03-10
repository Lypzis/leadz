package com.lypzis.lead_api.controller;

import com.lypzis.lead_api.service.LeadPublisherService;
import com.lypzis.lead_contracts.dto.LeadEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
public class WebhookController {

    private final LeadPublisherService publisherService;

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody LeadEventDTO request) {

        publisherService.publish(request);

        return ResponseEntity.accepted().build();
    }
}
