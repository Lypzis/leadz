package com.lypzis.lead_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    @Bean
    public OpenAPI leadzOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Leadz API")
                        .description("Multi-tenant WhatsApp automation backend")
                        .version("1.0.0"));
    }
}
