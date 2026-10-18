package br.com.lata.velha.shared.infrastructure.messaging.rabbitmq;

import br.com.lata.velha.shared.application.gateways.EventPublisher;
import br.com.lata.velha.shared.application.logging.Logger;
import br.com.lata.velha.shared.domain.events.DomainEvent;
import br.com.lata.velha.shared.infrastructure.messaging.EventSerializer;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMqEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange eventsExchange;
    private final EventSerializer serializer;
    private final Logger logger;

    public RabbitMqEventPublisher(RabbitTemplate rabbitTemplate, TopicExchange eventsExchange,
                                   EventSerializer serializer, Logger logger) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventsExchange = eventsExchange;
        this.serializer = serializer;
        this.logger = logger;
    }

    @Override
    public void publish(DomainEvent event) {
        String json = serializer.serialize(event);
        logger.logInfo("Publicando evento no RabbitMQ - eventType={}, eventId={}, exchange={}, routingKey={}",
                event.eventType(), event.eventId(), eventsExchange.getName(), event.eventType());
        rabbitTemplate.convertAndSend(eventsExchange.getName(), event.eventType(), json);
    }
}
