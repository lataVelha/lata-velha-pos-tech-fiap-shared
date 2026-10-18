package br.com.lata.velha.shared.infrastructure.messaging.rabbitmq;

import br.com.lata.velha.shared.application.logging.Logger;
import br.com.lata.velha.shared.infrastructure.messaging.EventHandlerRegistry;
import br.com.lata.velha.shared.infrastructure.messaging.EventSerializer;
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
        try {
            var envelope = serializer.deserializeEnvelope(json);
            registry.dispatch(envelope);
        } catch (Exception e) {
            logger.logError("Falha ao processar mensagem do RabbitMQ - payload=" + json, e);
        }
    }
}
