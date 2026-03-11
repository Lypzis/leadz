package com.lypzis.lead_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lypzis.lead_api.dto.MetaWebhookMessageDTO;
import com.lypzis.lead_api.exception.UnauthorizedException;
import com.lypzis.lead_contracts.dto.LeadEventDTO;
import com.lypzis.lead_domain.entity.Tenant;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    private static final String APP_SECRET = "test-app-secret";

    @Mock
    private LeadPublisherService leadPublisherService;

    @Mock
    private TenantService tenantService;

    @Captor
    private ArgumentCaptor<LeadEventDTO> eventCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(leadPublisherService, tenantService, objectMapper);
        ReflectionTestUtils.setField(webhookService, "whatsappAppSecret", APP_SECRET);
        ReflectionTestUtils.setField(webhookService, "whatsappVerifyToken", "verify-token");
    }

    @Test
    void receiveMessageShouldMapMetaPayloadAndPublishLeadEvent() throws Exception {
        MetaWebhookMessageDTO request = validMetaRequest();
        String rawPayload = objectMapper.writeValueAsString(request);
        String signature = signatureFor(rawPayload);

        Tenant tenant = new Tenant();
        tenant.setApiKey("tenant-key");
        tenant.setRequestsPerMinute(100);
        tenant.setActive(true);
        when(tenantService.resolveWebhookTenantByPhoneNumberId("123456123")).thenReturn(tenant);

        webhookService.receiveMessage(rawPayload, signature);

        verify(leadPublisherService).publish(eventCaptor.capture(), org.mockito.ArgumentMatchers.eq(tenant));
        LeadEventDTO event = eventCaptor.getValue();
        assertThat(event.getMessageId()).isEqualTo("ABGGFlA5Fpa");
        assertThat(event.getPhone()).isEqualTo("16315551181");
        assertThat(event.getMessage()).isEqualTo("this is a text message");
        assertThat(event.getCampaign()).isEqualTo("123456123");
    }

    @Test
    void receiveMessageShouldIgnorePayloadsWithoutTextMessage() throws Exception {
        MetaWebhookMessageDTO request = new MetaWebhookMessageDTO();
        request.setField("messages");
        request.setValue(new MetaWebhookMessageDTO.Value());
        String rawPayload = objectMapper.writeValueAsString(request);
        String signature = signatureFor(rawPayload);

        webhookService.receiveMessage(rawPayload, signature);

        verify(leadPublisherService, never()).publish(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(tenantService, never()).resolveWebhookTenantByPhoneNumberId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void receiveMessageShouldRejectInvalidSignature() throws Exception {
        MetaWebhookMessageDTO request = validMetaRequest();
        String rawPayload = objectMapper.writeValueAsString(request);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> webhookService.receiveMessage(rawPayload, "sha256=invalidsignature"));

        assertThat(exception.getMessage()).isEqualTo("Invalid webhook signature");
        verify(leadPublisherService, never()).publish(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void isVerifyWebhookValidShouldValidateTokenAndMode() {
        assertThat(webhookService.isVerifyWebhookValid("subscribe", "verify-token")).isTrue();
        assertThat(webhookService.isVerifyWebhookValid("subscribe", "wrong")).isFalse();
        assertThat(webhookService.isVerifyWebhookValid("other", "verify-token")).isFalse();
    }

    private MetaWebhookMessageDTO validMetaRequest() {
        MetaWebhookMessageDTO request = new MetaWebhookMessageDTO();
        request.setField("messages");

        MetaWebhookMessageDTO.Metadata metadata = new MetaWebhookMessageDTO.Metadata();
        metadata.setDisplayPhoneNumber("16505551111");
        metadata.setPhoneNumberId("123456123");

        MetaWebhookMessageDTO.Contact contact = new MetaWebhookMessageDTO.Contact();
        contact.setWaId("16315551181");
        MetaWebhookMessageDTO.Profile profile = new MetaWebhookMessageDTO.Profile();
        profile.setName("test user name");
        profile.setUsername("@testusername");
        contact.setProfile(profile);

        MetaWebhookMessageDTO.Text text = new MetaWebhookMessageDTO.Text();
        text.setBody("this is a text message");

        MetaWebhookMessageDTO.Message message = new MetaWebhookMessageDTO.Message();
        message.setFrom("16315551181");
        message.setId("ABGGFlA5Fpa");
        message.setTimestamp("1504902988");
        message.setType("text");
        message.setText(text);

        MetaWebhookMessageDTO.Value value = new MetaWebhookMessageDTO.Value();
        value.setMessagingProduct("whatsapp");
        value.setMetadata(metadata);
        value.setContacts(java.util.List.of(contact));
        value.setMessages(java.util.List.of(message));

        request.setValue(value);
        return request;
    }

    private String signatureFor(String rawPayload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
        return "sha256=" + HexFormat.of().formatHex(digest);
    }
}
