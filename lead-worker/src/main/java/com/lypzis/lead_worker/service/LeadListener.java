package com.lypzis.lead_worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;

import java.io.IOException;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.messaging.handler.annotation.Header;

import com.lypzis.lead_worker.config.RabbitConfig;
import com.lypzis.lead_contracts.dto.LeadDTO;
import com.rabbitmq.client.Channel;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadListener {

    private static final long MAX_RETRIES_BEFORE_DLQ = 1;

    private final LeadService leadService;
    // private final WhatsAppSender sender;
    private final RabbitTemplate rabbitTemplate;
    private final IdempotencyService idempotencyService;

    @RabbitListener(queues = RabbitConfig.MAIN_QUEUE)
    public void handleLead(LeadDTO event, Channel channel,
            @Header(name = AmqpHeaders.RETRY_COUNT, required = false) Long retryCount,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {

            if (idempotencyService.alreadyProcessed(event.getTenant(), event.getMessageId())) {

                log.info("Duplicate message ignored {}", event.getMessageId());
                channel.basicAck(tag, false);
                return;

            }

            leadService.processLead(event);

            idempotencyService.markProcessed(event.getTenant(), event.getMessageId());

            channel.basicAck(tag, false);

        } catch (Exception e) {
            if (retryCount != null && retryCount >= MAX_RETRIES_BEFORE_DLQ) {
                log.error("Sending message {} to DLQ after {} retries", event.getMessageId(), retryCount, e);
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.DLQ_ROUTING_KEY, event);
                return;
            }

            log.error("Failed processing lead {}", event.getMessageId(), e);

            channel.basicNack(tag, false, false);
        }
    }

}
