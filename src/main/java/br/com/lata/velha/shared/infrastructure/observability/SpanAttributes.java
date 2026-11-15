package br.com.lata.velha.shared.infrastructure.observability;

import io.opentelemetry.api.trace.Span;

/**
 * Atributos de negócio em spans (Story 19.2).
 *
 * Uso: {@code SpanAttributes.set("ordem_servico.id", id)}. Não faz nada se
 * não houver span válido (ex.: chamada fora de requisição).
 */
public final class SpanAttributes {

    private SpanAttributes() {
    }

    public static void set(String key, Object value) {
        if (key == null || value == null) {
            return;
        }
        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            span.setAttribute(key, String.valueOf(value));
        }
    }
}
