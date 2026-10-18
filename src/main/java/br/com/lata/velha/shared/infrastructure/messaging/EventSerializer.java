package br.com.lata.velha.shared.infrastructure.messaging;

import br.com.lata.velha.shared.domain.events.DomainEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component
public class EventSerializer {

    private final ObjectMapper objectMapper;

    public EventSerializer() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String serialize(DomainEvent event) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("eventId", event.eventId().toString());
        root.put("eventType", event.eventType());
        root.put("occurredAt", event.occurredAt().toString());
        root.set("payload", objectMapper.valueToTree(event));
        return writeAsString(root);
    }

    public EventEnvelope deserializeEnvelope(String json) {
        return readValue(json, EventEnvelope.class);
    }

    public <T extends DomainEvent> T deserializePayload(EventEnvelope envelope, Class<T> eventClass) {
        return objectMapper.convertValue(envelope.payload(), eventClass);
    }

    private String writeAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar evento para JSON", e);
        }
    }

    private <T> T readValue(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao desserializar envelope de evento: " + json, e);
        }
    }
}
