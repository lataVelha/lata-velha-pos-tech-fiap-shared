package br.com.lata.velha.shared.infrastructure.messaging;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato do EventSerializer (Story 15.1).
 *
 * O listener SQS depende deste formato: eventId/eventType/occurredAt no topo
 * e o payload do domínio em "payload".
 */
class EventSerializerContractTest {

    private final EventSerializer serializer = new EventSerializer();

    @Test
    @DisplayName("envelope preserva eventId, eventType e occurredAt no roundtrip")
    void devePreservarEnvelopeNoRoundtrip() {
        var event = new TestEvent(10L, "contrato");
        String json = serializer.serialize(event);

        var envelope = serializer.deserializeEnvelope(json);

        assertThat(envelope.eventId()).isEqualTo(event.eventId());
        assertThat(envelope.eventType()).isEqualTo("TesteEvento");
        assertThat(envelope.occurredAt()).isEqualTo(event.occurredAt());
    }

    @Test
    @DisplayName("campos desconhecidos no JSON são ignorados (tolerância a evolução do contrato)")
    void deveIgnorarCamposDesconhecidos() {
        var envelopeJson = """
                {
                  "eventId": "%s",
                  "eventType": "OrcamentoAprovado",
                  "occurredAt": "%s",
                  "campoNovoDoFuturo": "ignorar",
                  "payload": {
                    "osId": 42,
                    "itens": [],
                    "campoQueNaoExiste": true
                  }
                }
                """.formatted(UUID.randomUUID(), Instant.now());

        var envelope = serializer.deserializeEnvelope(envelopeJson);

        assertThat(envelope.eventType()).isEqualTo("OrcamentoAprovado");
        assertThat(envelope.payload().get("osId").asLong()).isEqualTo(42L);
    }

    @Test
    @DisplayName("payload inválido lança exceção e não é silenciado")
    void deveFalharComPayloadInvalido() {
        assertThatThrownBy(() -> serializer.deserializeEnvelope("{\"eventId\":\"nao-uuid\"}"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("payload contém os campos do evento, permitindo desserializar na classe concreta")
    void deveDesserializarPayloadConcreto() {
        var event = new TestEvent(7L, "payload");
        var envelope = serializer.deserializeEnvelope(serializer.serialize(event));

        var reconstructed = serializer.deserializePayload(envelope, TestEvent.class);

        assertThat(reconstructed.getOsId()).isEqualTo(7L);
        assertThat(reconstructed.getDescricao()).isEqualTo("payload");
    }
}
