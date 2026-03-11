package com.lypzis.lead_api.dto;

public record AuthResponseDTO(
        String accessToken,
        String tokenType,
        long expiresInSeconds) {
}
