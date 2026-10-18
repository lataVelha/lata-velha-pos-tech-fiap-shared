package br.com.lata.velha.shared.infrastructure.messaging.sqs;

import br.com.lata.velha.shared.application.logging.Logger;
import br.com.lata.velha.shared.infrastructure.messaging.EventHandlerRegistry;
import br.com.lata.velha.shared.infrastructure.messaging.EventSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "sqs")
public class SqsEventListener implements SmartLifecycle {

    private final SqsClient sqsClient;
    private final EventHandlerRegistry registry;
    private final EventSerializer serializer;
    private final Logger logger;

    @Value("${messaging.sqs.queue-url}")
    private String queueUrl;

    @Value("${messaging.sqs.wait-time-seconds:20}")
    private int waitTimeSeconds;

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
        if (executor != null) executor.shutdownNow();
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

    private void processMessage(Message message) {
        try {
            var envelope = serializer.deserializeEnvelope(message.body());
            registry.dispatch(envelope);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception e) {
            logger.logError("Falha ao processar mensagem do SQS, será reentregue - messageId=" + message.messageId(), e);
        }
    }
}
