package com.lypzis.lead_worker.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeadEventDTO {

    private int version = 1;
    private String messageId;
    private String phone;
    private String message;
    private String campaign;
    private String apiKey;
}