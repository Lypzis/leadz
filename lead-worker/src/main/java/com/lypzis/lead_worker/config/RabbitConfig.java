package com.lypzis.lead_worker.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.Binding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "lead.exchange";
    public static final String QUEUE = "lead.created.queue";
    public static final String ROUTING_KEY = "lead.created";

    @Bean
    public TopicExchange leadExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue leadQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding binding(Queue leadQueue, TopicExchange leadExchange) {
        return BindingBuilder
                .bind(leadQueue)
                .to(leadExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
