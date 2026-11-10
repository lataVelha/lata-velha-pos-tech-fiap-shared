package br.com.lata.velha.shared.infrastructure.messaging.sqs;

import br.com.lata.velha.shared.application.logging.Logger;
import br.com.lata.velha.shared.infrastructure.messaging.EventEnvelope;
import br.com.lata.velha.shared.infrastructure.messaging.EventHandlerRegistry;
import br.com.lata.velha.shared.infrastructure.messaging.EventSerializer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listener SQS com semântica at-least-once (Story 14.4):
 *
 * - a mensagem só é apagada DEPOIS que o handler termina com sucesso;
 * - falha no handler (ou payload inválido) mantém a mensagem na fila — ela
 *   volta a ficar visível após o visibility timeout e, na 5ª tentativa, o
 *   redrive policy a move para a DLQ;
 * - evento sem handler registrado é logado e não quebra o loop de polling;
 * - stop() encerra o executor e não deixa thread órfã.
 *
 * Story 18.3: quando `OTEL_INSTRUMENTATION_AWS_SDK_ENABLED=true` (default),
 * extrai o `traceparent` do message attribute e cria um span CONSUMER filho
 * do span PRODUCER publicado pelo billing/os/inventory.
 */
@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "sqs")
public class SqsEventListener implements SmartLifecycle {

    private static final TextMapPropagator PROPAGATOR = W3CTraceContextPropagator.getInstance();
    private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier == null ? List.of() : carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier == null ? null : carrier.get(key);
        }
    };
    private static final Tracer TRACER = GlobalOpenTelemetry.getTracer("lata-velha.messaging");

    private final SqsClient sqsClient;
    private final EventHandlerRegistry registry;
    private final EventSerializer serializer;
    private final Logger logger;

    @Value("${messaging.sqs.queue-url:}")
    private String queueUrl;

    @Value("${messaging.sqs.wait-time-seconds:20}")
    private int waitTimeSeconds;

    @Value("${OTEL_INSTRUMENTATION_AWS_SDK_ENABLED:true}")
    private boolean tracePropagationEnabled;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;

    public SqsEventListener(SqsClient sqsClient, EventHandlerRegistry registry,
                             EventSerializer serializer, Logger logger) {
        this.sqsClient = sqsClient;
        this.registry = registry;
        this.serializer = serializer;
        this.logger = logger;
    }

    @Override
    public void start() {
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException(
                    "MESSAGING_SQS_QUEUE_URL é obrigatório quando MESSAGING_PROVIDER=sqs. "
                            + "Sem ele o serviço subiria sem consumir eventos.");
        }
        if (!running.compareAndSet(false, true)) return;
        executor = Executors.newSingleThreadExecutor(r -> {
            var thread = new Thread(r, "sqs-event-listener");
            thread.setDaemon(true);
            return thread;
        });
        executor.submit(this::pollLoop);
        logger.logInfo("SqsEventListener iniciado - queueUrl={}", queueUrl);
    }

    @Override
    public void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.logInfo("SqsEventListener parado - queueUrl={}", queueUrl);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                receiveAndProcessBatch();
            } catch (Exception e) {
                logger.logError("Falha no ciclo de polling do SQS - queueUrl=" + queueUrl, e);
            }
        }
    }

    private void receiveAndProcessBatch() {
        var request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(waitTimeSeconds)
                .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();
        for (Message message : messages) {
            processMessage(message);
        }
    }

    // Package-private para teste (Story 14.4).
    void processMessage(Message message) {
        EventEnvelope envelope = null;
        Context parent = tracePropagationEnabled
                ? PROPAGATOR.extract(Context.current(), carrierOf(message), GETTER)
                : Context.current();
        Span span = TRACER.spanBuilder("SQS Process")
                .setParent(parent)
                .setSpanKind(SpanKind.CONSUMER)
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            envelope = serializer.deserializeEnvelope(message.body());
            span.updateName("SQS Process " + envelope.eventType());
            registry.dispatch(envelope);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
            logger.logInfo("Mensagem processada - eventId={}, eventType={}, messageId={}, queueUrl={}",
                    envelope.eventId(), envelope.eventType(), message.messageId(), queueUrl);
        } catch (Exception e) {
            span.recordException(e);
            logger.logError("Falha ao processar mensagem do SQS, será reentregue/DLQ - " +
                            "eventId=" + (envelope == null ? "desconhecido" : envelope.eventId()) +
                            ", eventType=" + (envelope == null ? "desconhecido" : envelope.eventType()) +
                            ", messageId=" + message.messageId() +
                            ", queueUrl=" + queueUrl, e);
        } finally {
            span.end();
        }
    }

    private Map<String, String> carrierOf(Message message) {
        if (message.messageAttributes() == null || message.messageAttributes().isEmpty()) {
            return Map.of();
        }
        Map<String, String> carrier = new HashMap<>();
        message.messageAttributes().forEach((key, value) -> {
            if (value.stringValue() != null) {
                carrier.put(key, value.stringValue());
            }
        });
        return carrier;
    }
}
