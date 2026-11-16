package br.com.lata.velha.shared.infrastructure.messaging.sqs;

import br.com.lata.velha.shared.application.logging.Logger;
import br.com.lata.velha.shared.infrastructure.messaging.EventEnvelope;
import br.com.lata.velha.shared.infrastructure.messaging.EventHandlerRegistry;
import br.com.lata.velha.shared.infrastructure.messaging.EventSerializer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsEventListenerTest {

    @Mock private SqsClient sqsClient;
    @Mock private EventHandlerRegistry registry;
    @Mock private EventSerializer serializer;
    @Mock private Logger logger;

    private SqsEventListener listener;

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123/lata-velha-os-service-events";
    private static final String TRACEPARENT =
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

    @BeforeEach
    void setUp() {
        listener = new SqsEventListener(sqsClient, registry, serializer, logger, OpenTelemetry.noop());
        ReflectionTestUtils.setField(listener, "queueUrl", QUEUE_URL);
        ReflectionTestUtils.setField(listener, "tracePropagationEnabled", true);
    }

    private Message message() {
        return Message.builder()
                .messageId("msg-1")
                .receiptHandle("receipt-1")
                .body("{\"eventId\":\"x\"}")
                .build();
    }

    private Message messageWithAttributes(Map<String, String> attributes) {
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        attributes.forEach((key, value) -> messageAttributes.put(key, MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(value)
                .build()));
        return Message.builder()
                .messageId("msg-1")
                .receiptHandle("receipt-1")
                .body("{\"eventId\":\"x\"}")
                .messageAttributes(messageAttributes)
                .build();
    }

    private EventEnvelope envelope() {
        return new EventEnvelope(UUID.randomUUID(), "OrcamentoAprovado", Instant.now(), null);
    }

    @Test
    @DisplayName("deve falhar rápido quando queue-url não está configurada")
    void deveFalharRapidoSemQueueUrl() {
        ReflectionTestUtils.setField(listener, "queueUrl", "");

        assertThatThrownBy(listener::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MESSAGING_SQS_QUEUE_URL");
    }

    @Test
    @DisplayName("o poller deve pedir message attributes e o receive count ao SQS")
    void devePedirMessageAttributesNoReceive() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of()).build());

        listener.receiveAndProcessBatch();

        var captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqsClient).receiveMessage(captor.capture());
        assertThat(captor.getValue().messageAttributeNames()).contains("All");
        assertThat(captor.getValue().messageSystemAttributeNamesAsStrings())
                .contains(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT.toString());
    }

    @Test
    @DisplayName("deve extrair o traceparent dos message attributes")
    void deveExtrairTraceparentDosMessageAttributes() {
        var message = messageWithAttributes(Map.of("traceparent", TRACEPARENT));

        assertThat(listener.carrierOf(message)).containsEntry("traceparent", TRACEPARENT);
    }

    @Test
    @DisplayName("o span do consumidor deve ser filho do span do produtor")
    void consumerSpanDeveSerFilhoDoProducer() {
        var exporter = InMemorySpanExporter.create();
        var provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        var openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(provider)
                .build();

        var producer = openTelemetry.getTracer("test").spanBuilder("SNS Publish")
                .setSpanKind(SpanKind.PRODUCER)
                .startSpan();
        var carrier = new HashMap<String, String>();
        W3CTraceContextPropagator.getInstance()
                .inject(Context.current().with(producer), carrier, Map::put);
        producer.end();

        var message = messageWithAttributes(Map.of("traceparent", carrier.get("traceparent")));
        when(serializer.deserializeEnvelope(any())).thenReturn(envelope());
        var tracedListener = new SqsEventListener(
                sqsClient, registry, serializer, logger, openTelemetry);
        ReflectionTestUtils.setField(tracedListener, "tracePropagationEnabled", true);

        try {
            tracedListener.processMessage(message);

            var consumer = exporter.getFinishedSpanItems().stream()
                    .filter(span -> span.getKind() == SpanKind.CONSUMER)
                    .findFirst()
                    .orElseThrow();
            assertThat(consumer.getTraceId()).isEqualTo(producer.getSpanContext().getTraceId());
            assertThat(consumer.getParentSpanId()).isEqualTo(producer.getSpanContext().getSpanId());
        } finally {
            provider.close();
        }
    }

    @Test
    @DisplayName("mensagem sem traceparent continua sendo processada como trace raiz")
    void mensagemSemTraceparentNaoQuebra() {
        when(serializer.deserializeEnvelope(any())).thenReturn(envelope());

        listener.processMessage(message());

        verify(registry).dispatch(any());
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("deve apagar a mensagem somente depois do handler concluir com sucesso")
    void deveApagarMensagemAposSucesso() {
        when(serializer.deserializeEnvelope(any())).thenReturn(envelope());

        listener.processMessage(message());

        verify(registry).dispatch(any());
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("não deve apagar a mensagem quando o handler lança exceção")
    void naoDeveApagarMensagemQuandoHandlerFalha() {
        when(serializer.deserializeEnvelope(any())).thenReturn(envelope());
        doThrow(new IllegalStateException("falha no handler"))
                .when(registry).dispatch(any());

        listener.processMessage(message());

        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
        verify(logger).logError(contains("eventType=OrcamentoAprovado"), any(Exception.class));
    }

    @Test
    @DisplayName("não deve apagar a mensagem quando o payload é inválido")
    void naoDeveApagarMensagemQuandoPayloadInvalido() {
        when(serializer.deserializeEnvelope(any()))
                .thenThrow(new IllegalStateException("json inválido"));

        listener.processMessage(message());

        verify(registry, never()).dispatch(any());
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
        verify(logger).logError(contains("eventType=desconhecido"), any(Exception.class));
    }

    @Test
    @DisplayName("evento sem handler registrado não quebra o loop e a mensagem é consumida")
    void eventoDesconhecidoNaoQuebraLoop() {
        when(serializer.deserializeEnvelope(any())).thenReturn(envelope());
        doNothing().when(registry).dispatch(any());

        listener.processMessage(message());

        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("start/stop controla o ciclo de vida e encerra o executor")
    void deveIniciarEPararListener() {
        listener.start();
        verify(logger).logInfo("SqsEventListener iniciado - queueUrl={}", QUEUE_URL);
        listener.stop();

        verify(logger).logInfo("SqsEventListener parado - queueUrl={}", QUEUE_URL);
        assertThat(listener.isRunning()).isFalse();
    }
}
