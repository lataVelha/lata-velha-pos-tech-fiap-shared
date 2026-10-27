package br.com.lata.velha.shared.infrastructure.messaging;

import br.com.lata.velha.shared.domain.events.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.stereotype.Component;

@Component
public class EventSerializer {

    private final ObjectMapper objectMapper;

    public EventSerializer() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule());

        AnnotationIntrospector forcePropertiesCreator = new JacksonAnnotationIntrospector() {
            @Override
            public JsonCreator.Mode findCreatorAnnotation(MapperConfig<?> config, Annotated a) {
                JsonCreator.Mode mode = super.findCreatorAnnotation(config, a);
                if (mode == null && a instanceof AnnotatedConstructor) {
                    return JsonCreator.Mode.PROPERTIES;
                }
                return mode;
            }
        };

        this.objectMapper = mapper
                .setAnnotationIntrospector(AnnotationIntrospector.pair(
                        forcePropertiesCreator, mapper.getDeserializationConfig().getAnnotationIntrospector()))
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
