package br.com.lata.velha.shared.domain.events;

import java.time.Instant;
import java.util.UUID;

public abstract class AbstractDomainEvent implements DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;

    protected AbstractDomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    protected AbstractDomainEvent(UUID eventId, Instant occurredAt) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
    }

    @Override
    public UUID eventId() {
        return eventId;
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }
}
