package com.lypzis.lead_contracts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeadDTO {

    private int version = 1;
    private String messageId;
    private String phone;
    private String message;
    private String campaign;
    private String tenant;
}
