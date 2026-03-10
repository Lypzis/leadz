package com.lypzis.lead_worker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.lypzis.lead_contracts.dto.LeadDTO;
import com.lypzis.lead_worker.config.RabbitConfig;
import com.lypzis.lead_worker.repository.LeadRepository;
import com.lypzis.lead_worker.repository.ProcessedMessageRepository;

@Testcontainers
@SpringBootTest
@Tag("integration")
class LeadListenerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("lead_worker_it")
            .withUsername("leaduser")
            .withPassword("leadpass");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management")
            .withAdminUser("admin")
            .withAdminPassword("admin");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("spring.rabbitmq.listener.simple.default-requeue-rejected", () -> "false");
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @BeforeEach
    void cleanState() {
        amqpAdmin.purgeQueue(RabbitConfig.MAIN_QUEUE, true);
        amqpAdmin.purgeQueue(RabbitConfig.RETRY_QUEUE, true);
        amqpAdmin.purgeQueue(RabbitConfig.DLQ, true);
        leadRepository.deleteAll();
        processedMessageRepository.deleteAll();
    }

    @Test
    void shouldConsumeRabbitMessageAndPersistLead() {
        LeadDTO event = new LeadDTO();
        event.setMessageId("msg-" + UUID.randomUUID());
        event.setPhone("+15550001111");
        event.setMessage("integration payload");
        event.setCampaign("integration-campaign");
        event.setTenant("tenant-a");

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event);

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var savedLead = leadRepository.findByTenantAndMessageId(event.getTenant(), event.getMessageId());
                    assertThat(savedLead).isPresent();
                    assertThat(savedLead.orElseThrow().getPhone()).isEqualTo(event.getPhone());
                    assertThat(savedLead.orElseThrow().getMessage()).isEqualTo(event.getMessage());
                    assertThat(savedLead.orElseThrow().getCampaign()).isEqualTo(event.getCampaign());
                    assertThat(savedLead.orElseThrow().getTenant()).isEqualTo(event.getTenant());
                    assertThat(processedMessageRepository.existsByTenantAndMessageId(
                            event.getTenant(),
                            event.getMessageId())).isTrue();
                });
    }

    @Test
    void shouldIgnoreDuplicateMessageUsingIdempotency() {
        LeadDTO event = new LeadDTO();
        event.setMessageId("msg-dup-" + UUID.randomUUID());
        event.setPhone("+15550009999");
        event.setMessage("integration payload");
        event.setCampaign("integration-campaign");
        event.setTenant("tenant-dup");

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event);

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(
                        processedMessageRepository.existsByTenantAndMessageId(
                                event.getTenant(),
                                event.getMessageId())).isTrue());

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event);

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertThat(leadRepository.count()).isEqualTo(1L);
                    assertThat(processedMessageRepository.count()).isEqualTo(1L);
                });
    }

    @Test
    void shouldAllowSameMessageIdForDifferentTenants() {
        String sharedMessageId = "msg-shared-" + UUID.randomUUID();

        LeadDTO tenantAEvent = new LeadDTO();
        tenantAEvent.setMessageId(sharedMessageId);
        tenantAEvent.setPhone("+15550001001");
        tenantAEvent.setMessage("payload A");
        tenantAEvent.setCampaign("campaign-a");
        tenantAEvent.setTenant("tenant-a");

        LeadDTO tenantBEvent = new LeadDTO();
        tenantBEvent.setMessageId(sharedMessageId);
        tenantBEvent.setPhone("+15550001002");
        tenantBEvent.setMessage("payload B");
        tenantBEvent.setCampaign("campaign-b");
        tenantBEvent.setTenant("tenant-b");

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, tenantAEvent);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, tenantBEvent);

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertThat(leadRepository.findByTenantAndMessageId("tenant-a", sharedMessageId)).isPresent();
                    assertThat(leadRepository.findByTenantAndMessageId("tenant-b", sharedMessageId)).isPresent();
                    assertThat(processedMessageRepository.existsByTenantAndMessageId("tenant-a", sharedMessageId)).isTrue();
                    assertThat(processedMessageRepository.existsByTenantAndMessageId("tenant-b", sharedMessageId)).isTrue();
                });
    }
}
