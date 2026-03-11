package com.lypzis.lead_api.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lypzis.lead_api.dto.MetaWebhookMessageDTO;
import com.lypzis.lead_api.exception.UnauthorizedException;
import com.lypzis.lead_contracts.dto.LeadEventDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private static final String SIGNATURE_PREFIX = "sha256=";

    private final LeadPublisherService publisherService;
    private final TenantService tenantService;
    private final ObjectMapper objectMapper;

    @Value("${whatsapp.verify-token}")
    private String whatsappVerifyToken;

    @Value("${whatsapp.app-secret}")
    private String whatsappAppSecret;

    public void receiveMessage(String rawPayload, String signatureHeader) {
        if (!isValidSignature(rawPayload, signatureHeader)) {
            log.warn("Rejected webhook due to invalid signature");
            throw new UnauthorizedException("Invalid webhook signature");
        }

        MetaWebhookMessageDTO request = parseMetaPayload(rawPayload);
        MetaWebhookMessageDTO.Value messageValue = resolveMessageValue(request);
        LeadEventDTO leadEvent = toLeadEvent(messageValue);
        if (leadEvent == null) {
            log.info("Webhook accepted but ignored: unsupported/empty payload. rootField={}, resolvedMessagesCount={}",
                    request != null ? request.getField() : null,
                    messageCount(messageValue));
            return;
        }

        String phoneNumberId = messageValue != null && messageValue.getMetadata() != null
                ? messageValue.getMetadata().getPhoneNumberId()
                : null;
        var tenant = tenantService.resolveWebhookTenantByPhoneNumberId(phoneNumberId);
        log.info("Webhook extracted messageId={}, phone={}, tenantPhoneNumberId={}, resolvedTenantId={}",
                leadEvent.getMessageId(),
                leadEvent.getPhone(),
                phoneNumberId,
                tenant != null ? tenant.getId() : null);
        publisherService.publish(leadEvent, tenant);
    }

    public boolean isVerifyWebhookValid(String mode, String verifyToken) {
        return "subscribe".equals(mode) && whatsappVerifyToken.equals(verifyToken);
    }

    private MetaWebhookMessageDTO parseMetaPayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, MetaWebhookMessageDTO.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Malformed webhook payload", ex);
        }
    }

    private LeadEventDTO toLeadEvent(MetaWebhookMessageDTO.Value value) {
        if (value == null) {
            log.debug("Ignoring webhook with null messages value block");
            return null;
        }

        MetaWebhookMessageDTO.Message message = firstOrNull(value.getMessages());
        if (message == null) {
            log.debug("Ignoring webhook with no messages entries");
            return null;
        }

        String messageId = message.getId();
        String phone = firstNonBlank(message.getFrom(), firstContactWaId(value.getContacts()));
        String textBody = message.getText() != null ? message.getText().getBody() : null;

        if (isBlank(messageId) || isBlank(phone) || isBlank(textBody)) {
            log.debug("Ignoring webhook missing mandatory fields: messageIdPresent={}, phonePresent={}, textPresent={}",
                    !isBlank(messageId),
                    !isBlank(phone),
                    !isBlank(textBody));
            return null;
        }

        LeadEventDTO event = new LeadEventDTO();
        event.setMessageId(messageId);
        event.setPhone(phone);
        event.setMessage(textBody);
        event.setCampaign(value.getMetadata() != null ? value.getMetadata().getPhoneNumberId() : null);
        return event;
    }

    private MetaWebhookMessageDTO.Value resolveMessageValue(MetaWebhookMessageDTO request) {
        if (request == null) {
            return null;
        }

        // Backward-compatible flat payload: { "field": "messages", "value": {...} }
        if ("messages".equalsIgnoreCase(request.getField()) && request.getValue() != null) {
            return request.getValue();
        }

        // Meta payload: { "object": "...", "entry":[{"changes":[{"field":"messages","value":{...}}]}] }
        if (request.getEntry() == null) {
            return null;
        }

        for (MetaWebhookMessageDTO.Entry entry : request.getEntry()) {
            if (entry == null || entry.getChanges() == null) {
                continue;
            }
            for (MetaWebhookMessageDTO.Change change : entry.getChanges()) {
                if (change != null && "messages".equalsIgnoreCase(change.getField()) && change.getValue() != null) {
                    return change.getValue();
                }
            }
        }

        return null;
    }

    private int messageCount(MetaWebhookMessageDTO.Value value) {
        if (value == null || value.getMessages() == null) {
            return 0;
        }
        return value.getMessages().size();
    }

    private String firstContactWaId(List<MetaWebhookMessageDTO.Contact> contacts) {
        MetaWebhookMessageDTO.Contact contact = firstOrNull(contacts);
        return contact != null ? contact.getWaId() : null;
    }

    private <T> T firstOrNull(List<T> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first;
        }
        return isBlank(second) ? null : second;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValidSignature(String rawPayload, String signatureHeader) {
        if (isBlank(signatureHeader) || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        String expected = computeSignature(rawPayload);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8));
    }

    private String computeSignature(String rawPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    whatsappAppSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKey);
            byte[] digest = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            return SIGNATURE_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Unable to verify webhook signature", ex);
        }
    }
}
