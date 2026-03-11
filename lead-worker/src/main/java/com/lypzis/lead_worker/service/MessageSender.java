package com.lypzis.lead_worker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

import com.lypzis.lead_worker.dto.WhatsAppMessagePayloadDTO;
import com.lypzis.lead_worker.exception.NonRetryableProcessingException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageSender implements WhatsAppSender {

    @Value("${whatsapp.token}")
    private String token;

    @Value("${whatsapp.phone-id}")
    private String phoneId;

    private final WebClient webClient = WebClient.create();

    @Override
    public void sendMessage(String phone, String message) {
        WhatsAppMessagePayloadDTO payload = WhatsAppMessagePayloadDTO.textMessage(phone, message);

        try {
            webClient.post()
                    .uri("https://graph.facebook.com/v22.0/" + phoneId + "/messages")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.error("WhatsApp send failed for phone={} status={} response={}",
                    phone,
                    ex.getStatusCode(),
                    responseBody,
                    ex);
            if (isNonRetryableMetaError(ex, responseBody)) {
                throw new NonRetryableProcessingException(
                        "Non-retryable WhatsApp send failure for phone " + phone,
                        ex);
            }
            throw ex;
        }

        log.info("WhatsApp message sent to {}", phone);
    }

    private boolean isNonRetryableMetaError(WebClientResponseException ex, String responseBody) {
        return ex.getStatusCode().value() == 400
                && responseBody != null
                && (responseBody.contains("\"code\":131030")
                        || responseBody.contains("Recipient phone number not in allowed list"));
    }
}
