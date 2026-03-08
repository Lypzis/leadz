package com.lypzis.lead_worker.dto;

import lombok.Data;

@Data
public class LeadEventDTO {

    private String messageId;
    private String phone;
    private String message;
    private String campaign;

}