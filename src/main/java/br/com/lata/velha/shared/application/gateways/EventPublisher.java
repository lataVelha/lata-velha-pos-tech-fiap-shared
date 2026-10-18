package br.com.lata.velha.shared.application.gateways;

import br.com.lata.velha.shared.domain.events.DomainEvent;

public interface EventPublisher {

    void publish(DomainEvent event);
}
