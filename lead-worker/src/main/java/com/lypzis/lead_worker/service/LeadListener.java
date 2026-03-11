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
import com.lypzis.lead_worker.exception.NonRetryableProcessingException;
import com.lypzis.lead_contracts.dto.LeadDTO;
import com.lypzis.lead_contracts.dto.ProcessingResultEnum;
import com.rabbitmq.client.Channel;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadListener {

    private static final long MAX_RETRIES_BEFORE_DLQ = 1;

    private final LeadService leadService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitConfig.MAIN_QUEUE)
    public void handleLead(LeadDTO event, Channel channel,
            @Header(name = AmqpHeaders.RETRY_COUNT, required = false) Long retryCount,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            ProcessingResultEnum processingResult = leadService.processLeadTransactionally(event);
            if (processingResult == ProcessingResultEnum.DUPLICATE_IGNORED) {
                log.info("Message {} already processed, acking duplicate delivery", event.getMessageId());
            }

            channel.basicAck(tag, false);

        } catch (NonRetryableProcessingException e) {
            log.error("Sending message {} directly to DLQ due to non-retryable processing error",
                    event.getMessageId(), e);
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.DLQ_ROUTING_KEY, event);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            if (retryCount != null && retryCount >= MAX_RETRIES_BEFORE_DLQ) {
                log.error("Sending message {} to DLQ after {} retries", event.getMessageId(), retryCount, e);
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.DLQ_ROUTING_KEY, event);
                channel.basicAck(tag, false);
                return;
            }

            log.error("Failed processing lead {}", event.getMessageId(), e);

            channel.basicNack(tag, false, false);
        }
    }

}
