package br.com.lata.velha.shared.infrastructure.messaging;

import java.nio.charset.StandardCharsets;

/**
 * Atributos de semântica de mensageria (Story 19.1).
 *
 * Os helpers são puros para poderem ser testados sem AWS/SNS/SQS.
 */
public final class MessagingAttributes {

    private MessagingAttributes() {
    }

    /** Extrai o nome do tópico/fila de um ARN ou URL. */
    public static String destinationName(String arnOrUrl) {
        if (arnOrUrl == null || arnOrUrl.isBlank()) {
            return "unknown";
        }
        String value = arnOrUrl.endsWith("/") ? arnOrUrl.substring(0, arnOrUrl.length() - 1) : arnOrUrl;
        int separator = Math.max(value.lastIndexOf(':'), value.lastIndexOf('/'));
        return separator >= 0 ? value.substring(separator + 1) : value;
    }

    public static int bodySize(String body) {
        return body == null ? 0 : body.getBytes(StandardCharsets.UTF_8).length;
    }
}
