package br.com.lata.velha.shared.infrastructure.messaging.sqs;

import br.com.lata.velha.shared.application.gateways.EventPublisher;
import br.com.lata.velha.shared.application.logging.Logger;
import br.com.lata.velha.shared.domain.events.DomainEvent;
import br.com.lata.velha.shared.infrastructure.messaging.EventSerializer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Publica eventos de domínio no SNS.
 *
 * Story 18.3: quando `OTEL_INSTRUMENTATION_AWS_SDK_ENABLED=true` (default),
 * injeta o `traceparent` W3C nos message attributes, permitindo que o
 * consumidor continue o mesmo trace. O nome da classe é legado (o destino é
 * o SNS, não o SQS).
 */
@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "sqs")
public class SqsEventPublisher implements EventPublisher {

    private static final TextMapPropagator PROPAGATOR = W3CTraceContextPropagator.getInstance();
    private static final TextMapSetter<Map<String, String>> SETTER = Map::put;

    private final SnsClient snsClient;
    private final EventSerializer serializer;
    private final Logger logger;

    @Value("${messaging.sqs.topic-arn:}")
    private String topicArn;

    @Value("${OTEL_INSTRUMENTATION_AWS_SDK_ENABLED:true}")
    private boolean tracePropagationEnabled;

    public SqsEventPublisher(SnsClient snsClient, EventSerializer serializer, Logger logger) {
        this.snsClient = snsClient;
        this.serializer = serializer;
        this.logger = logger;
    }

    @Override
    public void publish(DomainEvent event) {
        String json = serializer.serialize(event);
        logger.logInfo("Publicando evento no SNS - eventType={}, eventId={}, topicArn={}",
                event.eventType(), event.eventId(), topicArn);

        var attributes = new HashMap<String, MessageAttributeValue>();
        attributes.put("eventType", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(event.eventType())
                .build());

        Tracer tracer = GlobalOpenTelemetry.getTracer("lata-velha.messaging");
        Span span = tracer.spanBuilder("SNS Publish " + event.eventType())
                .setSpanKind(SpanKind.PRODUCER)
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            if (tracePropagationEnabled) {
                Map<String, String> carrier = new HashMap<>();
                PROPAGATOR.inject(Context.current(), carrier, SETTER);
                carrier.forEach((key, value) -> attributes.put(key, MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(value)
                        .build()));
            }

            snsClient.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(json)
                    .messageAttributes(attributes)
                    .build());
        } catch (RuntimeException e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
