package com.lypzis.lead_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeadEventDTO {

    @NotBlank
    private String messageId;

    @NotBlank
    private String phone;

    @NotBlank
    private String message;

    private String campaign;
}