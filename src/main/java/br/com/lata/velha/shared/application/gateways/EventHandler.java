package br.com.lata.velha.shared.application.gateways;

import br.com.lata.velha.shared.domain.events.DomainEvent;

public interface EventHandler<T extends DomainEvent> {

    String eventType();

    Class<T> eventClass();

    void handle(T event);
}
