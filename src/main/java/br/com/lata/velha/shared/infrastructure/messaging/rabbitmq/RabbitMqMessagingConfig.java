package br.com.lata.velha.shared.infrastructure.messaging.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMqMessagingConfig {

    @Value("${messaging.rabbitmq.exchange:lata-velha.events}")
    private String exchangeName;

    @Value("${messaging.rabbitmq.queue:${spring.application.name}.events}")
    private String queueName;

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public DirectExchange eventsDeadLetterExchange() {
        return new DirectExchange(deadLetterExchangeName(), true, false);
    }

    @Bean
    public Queue eventsDeadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueueName()).build();
    }

    @Bean
    public Binding eventsDeadLetterBinding(Queue eventsDeadLetterQueue, DirectExchange eventsDeadLetterExchange) {
        return BindingBuilder.bind(eventsDeadLetterQueue)
                .to(eventsDeadLetterExchange)
                .with(deadLetterQueueName());
    }

    @Bean
    public Queue eventsQueue() {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(deadLetterExchangeName())
                .deadLetterRoutingKey(deadLetterQueueName())
                .build();
    }

    @Bean
    public Binding eventsBinding(Queue eventsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(eventsQueue).to(eventsExchange).with("#");
    }

    private String deadLetterExchangeName() {
        return exchangeName + ".dlx";
    }

    private String deadLetterQueueName() {
        return queueName + ".dlq";
    }
}
