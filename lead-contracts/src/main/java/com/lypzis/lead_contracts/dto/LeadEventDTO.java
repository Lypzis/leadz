package com.lypzis.lead_contracts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeadEventDTO {

    private int version = 1;

    @NotBlank
    private String messageId;

    @NotBlank
    private String phone;

    @NotBlank
    private String message;

    private String campaign;

    private String apiKey;
}
