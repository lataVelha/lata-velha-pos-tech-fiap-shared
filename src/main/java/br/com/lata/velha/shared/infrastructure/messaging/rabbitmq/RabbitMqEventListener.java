package br.com.lata.velha.shared.infrastructure.messaging.rabbitmq;

import br.com.lata.velha.shared.application.logging.Logger;
import br.com.lata.velha.shared.infrastructure.messaging.EventEnvelope;
import br.com.lata.velha.shared.infrastructure.messaging.EventHandlerRegistry;
import br.com.lata.velha.shared.infrastructure.messaging.EventSerializer;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMqEventListener {

    private final EventHandlerRegistry registry;
    private final EventSerializer serializer;
    private final Logger logger;

    public RabbitMqEventListener(EventHandlerRegistry registry, EventSerializer serializer, Logger logger) {
        this.registry = registry;
        this.serializer = serializer;
        this.logger = logger;
    }

    @RabbitListener(queues = "${messaging.rabbitmq.queue:${spring.application.name}.events}")
    public void onMessage(String json) {
        EventEnvelope envelope;
        try {
            envelope = serializer.deserializeEnvelope(json);
        } catch (RuntimeException e) {
            logger.logError("Envelope de evento inválido, enviando direto para a DLQ - payload=" + json, e);
            throw new AmqpRejectAndDontRequeueException("Envelope de evento inválido", e);
        }

        try {
            registry.dispatch(envelope);
        } catch (RuntimeException e) {
            logger.logError("Falha ao processar evento - eventType=" + envelope.eventType()
                    + ", eventId=" + envelope.eventId(), e);
            throw e;
        }
    }
}
