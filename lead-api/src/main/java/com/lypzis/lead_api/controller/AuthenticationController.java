package com.lypzis.lead_api.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lypzis.lead_api.dto.AuthResponseDTO;
import com.lypzis.lead_api.dto.LoginRequestDTO;
import com.lypzis.lead_api.service.AuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Admin authentication endpoints")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Authenticates an admin user and returns a JWT access token")
    public AuthResponseDTO login(@RequestBody LoginRequestDTO request) {
        return authenticationService.login(request);
    }
}
