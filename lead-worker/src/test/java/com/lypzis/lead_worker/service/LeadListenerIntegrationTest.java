package com.lypzis.lead_worker.service;

import com.lypzis.lead_worker.config.RabbitConfig;
import com.lypzis.lead_worker.dto.LeadEventDTO;
import com.lypzis.lead_worker.repository.LeadRepository;
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
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private LeadRepository leadRepository;

    @BeforeEach
    void cleanDatabase() {
        leadRepository.deleteAll();
    }

    @Test
    void shouldConsumeRabbitMessageAndPersistLead() {
        LeadEventDTO event = new LeadEventDTO();
        event.setMessageId("msg-" + UUID.randomUUID());
        event.setPhone("+15550001111");
        event.setMessage("integration payload");
        event.setCampaign("integration-campaign");

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event);

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var savedLead = leadRepository.findByMessageId(event.getMessageId());
                    assertThat(savedLead).isPresent();
                    assertThat(savedLead.orElseThrow().getPhone()).isEqualTo(event.getPhone());
                    assertThat(savedLead.orElseThrow().getMessage()).isEqualTo(event.getMessage());
                    assertThat(savedLead.orElseThrow().getCampaign()).isEqualTo(event.getCampaign());
                });
    }
}
