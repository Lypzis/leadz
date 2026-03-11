package com.lypzis.lead_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.lypzis.lead_api.service.WebhookService;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock
    private WebhookService webhookService;

    @Test
    void receiveMessageShouldDelegateToService() {
        WebhookController controller = new WebhookController(webhookService);

        var response = controller.receiveMessage("{\"field\":\"messages\"}", "sha256=sig");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(webhookService).receiveMessage("{\"field\":\"messages\"}", "sha256=sig");
    }

    @Test
    void verifyWebhookShouldReturnOkWhenServiceValidatesRequest() {
        WebhookController controller = new WebhookController(webhookService);
        when(webhookService.isVerifyWebhookValid("subscribe", "token")).thenReturn(true);

        var response = controller.verifyWebhook("subscribe", "token", "challenge");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("challenge");
    }

    @Test
    void verifyWebhookShouldReturnForbiddenWhenServiceRejectsRequest() {
        WebhookController controller = new WebhookController(webhookService);
        when(webhookService.isVerifyWebhookValid("subscribe", "wrong-token")).thenReturn(false);

        var response = controller.verifyWebhook("subscribe", "wrong-token", "challenge");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
