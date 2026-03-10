package com.lypzis.lead_worker.service;

import com.lypzis.lead_worker.config.RabbitConfig;
import com.lypzis.lead_worker.entity.Tenant;
import com.lypzis.lead_worker.repository.LeadRepository;
import com.lypzis.lead_worker.repository.TenantRepository;
import com.lypzis.lead_contracts.dto.LeadEventDTO;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
    private TenantRepository tenantRepository;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @BeforeEach
    void cleanDatabase() {
        amqpAdmin.purgeQueue(RabbitConfig.MAIN_QUEUE, true);
        amqpAdmin.purgeQueue(RabbitConfig.RETRY_QUEUE, true);
        amqpAdmin.purgeQueue(RabbitConfig.DLQ, true);
        leadRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    void shouldConsumeRabbitMessageAndPersistLead() {
        Tenant tenant = Tenant.builder()
                .name("Integration Tenant")
                .apiKey("it-tenant-key")
                .build();
        Tenant savedTenant = tenantRepository.save(tenant);

        LeadEventDTO event = new LeadEventDTO();
        event.setMessageId("msg-" + UUID.randomUUID());
        event.setPhone("+15550001111");
        event.setMessage("integration payload");
        event.setCampaign("integration-campaign");
        event.setApiKey("it-tenant-key");

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event);

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var savedLead = leadRepository.findByMessageId(event.getMessageId());
                    assertThat(savedLead).isPresent();
                    assertThat(savedLead.orElseThrow().getPhone()).isEqualTo(event.getPhone());
                    assertThat(savedLead.orElseThrow().getMessage()).isEqualTo(event.getMessage());
                    assertThat(savedLead.orElseThrow().getCampaign()).isEqualTo(event.getCampaign());
                    assertThat(savedLead.orElseThrow().getTenant()).isNotNull();
                    Long leadTenantId = savedLead.orElseThrow().getTenant().getId();
                    assertThat(leadTenantId).isEqualTo(savedTenant.getId());
                    assertThat(tenantRepository.findById(leadTenantId))
                            .isPresent()
                            .get()
                            .extracting(Tenant::getApiKey)
                            .isEqualTo("it-tenant-key");
                });
    }

    @Test
    void shouldSendFailedMessageToDlqWhenTenantIsMissing() {
        LeadEventDTO event = new LeadEventDTO();
        event.setMessageId("msg-dlq-" + UUID.randomUUID());
        event.setPhone("+15550002222");
        event.setMessage("this should fail");
        event.setCampaign("integration-campaign");
        event.setApiKey("missing-tenant-api-key");

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event);

        await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    var dlqProps = amqpAdmin.getQueueProperties(RabbitConfig.DLQ);
                    assertThat(dlqProps).isNotNull();
                    Integer messageCount = (Integer) dlqProps.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
                    assertThat(messageCount).isNotNull();
                    assertThat(messageCount).isGreaterThan(0);
                });

        assertThat(leadRepository.findByMessageId(event.getMessageId())).isEmpty();
    }
}
