package com.lypzis.lead_worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.lypzis.lead_worker.config.RabbitConfig;
import com.lypzis.lead_worker.dto.LeadEventDTO;

@Service
@Slf4j
public class LeadListener {

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void handleLead(LeadEventDTO event) {

        log.info("Received lead event: {}", event);

        // Later:
        // save to database
        // routing logic
        // trigger follow-up

    }
}
