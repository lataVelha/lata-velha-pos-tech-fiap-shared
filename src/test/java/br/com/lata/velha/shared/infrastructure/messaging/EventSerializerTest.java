package br.com.lata.velha.shared.infrastructure.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventSerializerTest {

    private final EventSerializer serializer = new EventSerializer();

    @Test
    @DisplayName("deve serializar um evento com eventId/eventType/occurredAt no topo e o payload próprio")
    void deveSerializarEventoComEnvelope() {
        var event = new TestEvent(10L, "diagnostico finalizado");

        String json = serializer.serialize(event);

        assertThat(json).contains(event.eventId().toString())
                .contains("\"eventType\":\"TesteEvento\"")
                .contains("\"osId\":10")
                .contains("\"descricao\":\"diagnostico finalizado\"");
    }

    @Test
    @DisplayName("deve desserializar o envelope de volta com os mesmos eventId/eventType/occurredAt")
    void deveDesserializarEnvelope() {
        var event = new TestEvent(10L, "diagnostico finalizado");
        String json = serializer.serialize(event);

        var envelope = serializer.deserializeEnvelope(json);

        assertThat(envelope.eventId()).isEqualTo(event.eventId());
        assertThat(envelope.eventType()).isEqualTo("TesteEvento");
        assertThat(envelope.occurredAt()).isEqualTo(event.occurredAt());
    }

    @Test
    @DisplayName("deve reconstruir o payload na classe concreta do evento")
    void deveDesserializarPayload() {
        var event = new TestEvent(10L, "diagnostico finalizado");
        String json = serializer.serialize(event);
        var envelope = serializer.deserializeEnvelope(json);

        var reconstructed = serializer.deserializePayload(envelope, TestEvent.class);

        assertThat(reconstructed.getOsId()).isEqualTo(10L);
        assertThat(reconstructed.getDescricao()).isEqualTo("diagnostico finalizado");
    }
}
