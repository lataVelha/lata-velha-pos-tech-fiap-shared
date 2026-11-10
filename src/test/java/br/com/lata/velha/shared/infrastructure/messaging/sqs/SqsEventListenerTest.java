package br.com.lata.velha.shared.infrastructure.messaging.sqs;

import br.com.lata.velha.shared.application.logging.Logger;
import br.com.lata.velha.shared.infrastructure.messaging.EventEnvelope;
import br.com.lata.velha.shared.infrastructure.messaging.EventHandlerRegistry;
import br.com.lata.velha.shared.infrastructure.messaging.EventSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    @BeforeEach
    void setUp() {
        listener = new SqsEventListener(sqsClient, registry, serializer, logger);
        ReflectionTestUtils.setField(listener, "queueUrl", QUEUE_URL);
    }

    private Message message() {
        return Message.builder()
                .messageId("msg-1")
                .receiptHandle("receipt-1")
                .body("{\"eventId\":\"x\"}")
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
        org.assertj.core.api.Assertions.assertThat(listener.isRunning()).isFalse();
    }
}
