package br.com.lata.velha.shared.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        JsonNode payload
) {
}
