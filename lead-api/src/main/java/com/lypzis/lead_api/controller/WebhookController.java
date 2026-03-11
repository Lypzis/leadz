package com.lypzis.lead_api.controller;

import com.lypzis.lead_api.service.WebhookService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "WhatsApp webhook endpoints")
public class WebhookController {

    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";
    private final WebhookService webhookService;

    @PostMapping
    @Operation(summary = "Receive WhatsApp event", description = "Receives incoming Meta WhatsApp payloads and publishes extracted text messages to the internal queue")
    @SecurityRequirements
    @Hidden
    public ResponseEntity<Void> receiveMessage(
            @RequestBody String rawPayload,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signatureHeader) {

        webhookService.receiveMessage(rawPayload, signatureHeader);

        return ResponseEntity.accepted().build();
    }

    @GetMapping
    @Hidden
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {

        if (webhookService.isVerifyWebhookValid(mode, verifyToken)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
