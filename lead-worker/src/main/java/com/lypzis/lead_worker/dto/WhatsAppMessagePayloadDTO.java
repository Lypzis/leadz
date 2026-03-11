package com.lypzis.lead_worker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WhatsAppMessagePayloadDTO {

    @JsonProperty("messaging_product")
    private String messagingProduct;

    @JsonProperty("recipient_type")
    private String recipientType;

    private String to;
    private String type;
    private TextPayload text;

    public static WhatsAppMessagePayloadDTO textMessage(String to, String body) {
        return new WhatsAppMessagePayloadDTO(
                "whatsapp",
                "individual",
                to,
                "text",
                new TextPayload(body));
    }

    @Data
    @AllArgsConstructor
    public static class TextPayload {
        private String body;
    }
}
