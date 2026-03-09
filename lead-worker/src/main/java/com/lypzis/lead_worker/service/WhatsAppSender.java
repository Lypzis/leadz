package com.lypzis.lead_worker.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

// TODO this is a mock right, later choose an API: Z-API, Meta or Evolution
@Service
@Slf4j
public class WhatsAppSender {

    public void sendMessage(String phone, String message) {

        log.info("Sending WhatsApp message to {}: {}", phone, message);

    }
}
