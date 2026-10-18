package br.com.lata.velha.shared.domain.events;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();

    String eventType();

    Instant occurredAt();
}
