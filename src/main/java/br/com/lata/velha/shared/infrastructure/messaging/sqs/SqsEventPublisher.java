package br.com.lata.velha.shared.infrastructure.messaging.sqs;

import br.com.lata.velha.shared.application.gateways.EventPublisher;
import br.com.lata.velha.shared.application.logging.Logger;
import br.com.lata.velha.shared.domain.events.DomainEvent;
import br.com.lata.velha.shared.infrastructure.messaging.EventSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "sqs")
public class SqsEventPublisher implements EventPublisher {

    private final SnsClient snsClient;
    private final EventSerializer serializer;
    private final Logger logger;

    @Value("${messaging.sqs.topic-arn}")
    private String topicArn;

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

        var request = PublishRequest.builder()
                .topicArn(topicArn)
                .message(json)
                .messageAttributes(Map.of(
                        "eventType", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(event.eventType())
                                .build()
                ))
                .build();

        snsClient.publish(request);
    }
}
