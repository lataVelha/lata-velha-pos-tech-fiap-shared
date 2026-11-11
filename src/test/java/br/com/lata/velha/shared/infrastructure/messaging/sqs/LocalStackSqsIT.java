package br.com.lata.velha.shared.infrastructure.messaging.sqs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração SNS/SQS com LocalStack (Story 15.2).
 *
 * Roda apenas quando LOCALSTACK_ENDPOINT está definido (ex.:
 * http://localhost:4566). No CI, o job sobe o service container do
 * LocalStack; localmente, use `docker compose -f docker-compose.test.yml up -d`.
 *
 * Não usa credenciais AWS reais.
 */
@EnabledIfEnvironmentVariable(named = "LOCALSTACK_ENDPOINT", matches = ".+")
class LocalStackSqsIT {

    private static final String MESSAGE = """
            {"eventId":"11111111-1111-1111-1111-111111111111","eventType":"OrcamentoAprovado","occurredAt":"2026-01-01T00:00:00Z","payload":{"osId":42,"itens":[{"execucaoServicoId":10,"pecaId":100,"quantidade":2}]}}
            """;

    private SqsClient sqs;
    private SnsClient sns;
    private String queueUrl;
    private String topicArn;

    @BeforeEach
    void setUp() {
        var endpoint = URI.create(System.getenv("LOCALSTACK_ENDPOINT"));
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test"));

        sqs = SqsClient.builder()
                .endpointOverride(endpoint)
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .build();

        sns = SnsClient.builder()
                .endpointOverride(endpoint)
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .build();

        var suffix = UUID.randomUUID().toString().substring(0, 8);
        queueUrl = sqs.createQueue(CreateQueueRequest.builder()
                .queueName("lata-velha-os-service-events-" + suffix)
                .attributes(Map.of(QueueAttributeName.VISIBILITY_TIMEOUT, "1"))
                .build()).queueUrl();

        var queueArn = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build())
                .attributes().get(QueueAttributeName.QUEUE_ARN);

        topicArn = sns.createTopic(r -> r.name("lata-velha-events-" + suffix)).topicArn();

        sns.subscribe(SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("sqs")
                .endpoint(queueArn)
                .attributes(Map.of(
                        "RawMessageDelivery", "true",
                        "FilterPolicyScope", "MessageAttributes",
                        "FilterPolicy", "{\"eventType\":[\"OrcamentoAprovado\"]}"
                ))
                .build());
    }

    @AfterEach
    void tearDown() {
        if (queueUrl != null) {
            sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
        }
        if (topicArn != null) {
            sns.deleteTopic(r -> r.topicArn(topicArn));
        }
        if (sqs != null) sqs.close();
        if (sns != null) sns.close();
    }

    @Test
    @DisplayName("publish → raw delivery + filter policy entrega o envelope na fila")
    void deveEntregarEventoFiltradoComRawDelivery() {
        publish("OrcamentoAprovado");

        var message = awaitMessage(Duration.ofSeconds(10));

        assertThat(message).isNotNull();
        assertThat(message.body()).isEqualTo(MESSAGE.trim());
    }

    @Test
    @DisplayName("evento com eventType fora do filter policy não é entregue")
    void naoDeveEntregarEventoForaDoFiltro() {
        publish("ServiceEnded");

        var message = sqs.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(1)
                .build()).messages();

        assertThat(message).isEmpty();
    }

    @Test
    @DisplayName("mensagem não deletada volta a ficar visível após o visibility timeout")
    void deveReentregarMensagemNaoDeletada() {
        publish("OrcamentoAprovado");
        var first = awaitMessage(Duration.ofSeconds(10));
        assertThat(first).isNotNull();

        // Simula falha do handler: não deleta e força visibilidade imediata.
        sqs.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(first.receiptHandle())
                .visibilityTimeout(0)
                .build());

        var second = awaitMessage(Duration.ofSeconds(10));
        assertThat(second).isNotNull();
        assertThat(second.body()).isEqualTo(first.body());
    }

    private void publish(String eventType) {
        sns.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .message(MESSAGE.trim())
                .messageAttributes(Map.of(
                        "eventType", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(eventType)
                                .build()))
                .build());
    }

    private Message awaitMessage(Duration timeout) {
        var deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            var messages = sqs.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(1)
                    .build()).messages();
            if (!messages.isEmpty()) {
                return messages.getFirst();
            }
        }
        return null;
    }
}
