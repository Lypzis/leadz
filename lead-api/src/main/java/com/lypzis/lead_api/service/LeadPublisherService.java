package com.lypzis.lead_api.service;

import com.lypzis.lead_api.config.RabbitConfig;
import com.lypzis.lead_api.dto.LeadEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LeadPublisherService {

    private final RabbitTemplate rabbitTemplate;

    public void publish(LeadEventDTO event) {

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY,
                event
        );
    }
}
