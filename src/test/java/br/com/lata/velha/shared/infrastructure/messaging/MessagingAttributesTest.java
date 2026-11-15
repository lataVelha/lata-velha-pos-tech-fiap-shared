package br.com.lata.velha.shared.infrastructure.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagingAttributesTest {

    @Test
    void extraiNomeDoTopicoDoArn() {
        assertEquals("lata-velha-os-service-events",
                MessagingAttributes.destinationName("arn:aws:sns:us-east-1:123456789012:lata-velha-os-service-events"));
    }

    @Test
    void extraiNomeDaFilaDaUrl() {
        assertEquals("lata-velha-billing-service-events",
                MessagingAttributes.destinationName("https://sqs.us-east-1.amazonaws.com/123456789012/lata-velha-billing-service-events"));
    }

    @Test
    void trataVazio() {
        assertEquals("unknown", MessagingAttributes.destinationName(null));
        assertEquals("unknown", MessagingAttributes.destinationName(""));
    }

    @Test
    void calculaTamanhoDoBodyEmUtf8() {
        assertEquals(0, MessagingAttributes.bodySize(null));
        assertEquals(5, MessagingAttributes.bodySize("hello"));
        assertEquals(2, MessagingAttributes.bodySize("é"));
    }
}
