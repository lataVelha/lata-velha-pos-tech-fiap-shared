package br.com.lata.velha.shared.infrastructure.messaging;

import br.com.lata.velha.shared.application.gateways.EventHandler;
import br.com.lata.velha.shared.application.logging.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EventHandlerRegistry {

    private final Map<String, EventHandler<?>> handlersByEventType;
    private final EventSerializer serializer;
    private final Logger logger;

    public EventHandlerRegistry(List<EventHandler<?>> handlers, EventSerializer serializer, Logger logger) {
        this.handlersByEventType = handlers.stream()
                .collect(Collectors.toMap(EventHandler::eventType, h -> h));
        this.serializer = serializer;
        this.logger = logger;
    }

    public void dispatch(EventEnvelope envelope) {
        var handler = handlersByEventType.get(envelope.eventType());
        if (handler == null) {
            logger.logDebug("Nenhum handler registrado para evento - eventType={}, eventId={}",
                    envelope.eventType(), envelope.eventId());
            return;
        }
        invoke(handler, envelope);
    }

    @SuppressWarnings("unchecked")
    private <T extends br.com.lata.velha.shared.domain.events.DomainEvent> void invoke(EventHandler<?> handler, EventEnvelope envelope) {
        var typedHandler = (EventHandler<T>) handler;
        T event = (T) serializer.deserializePayload(envelope, handler.eventClass());
        logger.logInfo("Processando evento - eventType={}, eventId={}, handler={}",
                envelope.eventType(), envelope.eventId(), handler.getClass().getSimpleName());
        typedHandler.handle(event);
    }
}
